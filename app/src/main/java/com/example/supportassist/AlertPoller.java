package com.example.supportassist;

import android.content.Context;
import android.util.Log;
import dagger.hilt.android.qualifiers.ApplicationContext;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Replaces SocketManager (a Socket.io client pointed at a server that was
 * never built — this backend is PHP-FPM, not Node, so a WebSocket server was
 * never the right call here) and DataRepository's old fixed 15s polling loop.
 *
 * Talks to GET /api/alerts/poll: a single blocking HTTP call that returns the
 * moment something changes (or after ~25s with nothing). Loop = call, look at
 * what came back, call again — see the backend's RUNNING.md for the full
 * explanation and its real scaling limits.
 *
 * Deliberately only runs while the app is foregrounded (start()/stop() are
 * called from MainActivity's onStart()/onStop()). Background delivery is
 * FCM's job, not this — an infinite background long-poll loop would drain
 * battery for no benefit once the user has left the app.
 */
@Singleton
public class AlertPoller {
    private static final String TAG = "AlertPoller";
    private static final int TIMEOUT_SECONDS = 25;
    private static final long RETRY_DELAY_MS = 5_000; // backoff when offline, so a dead network doesn't spin-loop

    private final ApiService apiService;
    private final Context appContext;

    private volatile boolean running = false;
    private volatile Call<ApiResponse<AlertPollResponse>> currentCall;
    private String since;

    @Inject
    public AlertPoller(ApiService apiService, @ApplicationContext Context appContext) {
        this.apiService = apiService;
        this.appContext = appContext;
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        since = isoNow();
        new Thread(this::loop, "alert-poller").start();
    }

    public synchronized void stop() {
        running = false;
        if (currentCall != null) {
            currentCall.cancel(); // unblocks the thread immediately instead of waiting out the timeout
        }
    }

    private void loop() {
        while (running) {
            Call<ApiResponse<AlertPollResponse>> call = apiService.pollAlerts(since, TIMEOUT_SECONDS);
            currentCall = call;
            try {
                Response<ApiResponse<AlertPollResponse>> response = call.execute(); // blocks this thread, not the UI thread
                if (!running) break; // stop() was called while we were waiting

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AlertPollResponse body = response.body().getData();
                    since = body.getServerTime();
                    if (body.getAlerts() != null && !body.getAlerts().isEmpty()) {
                        Log.d(TAG, body.getAlerts().size() + " new alert(s)");
                        DataRepository.getInstance(appContext).syncDataSilently();
                    }
                    // Empty result is normal — the server just hit its 25s
                    // timeout with nothing new. Loop straight back around,
                    // no extra delay needed.
                } else {
                    sleepBeforeRetry();
                }
            } catch (java.io.IOException e) {
                // Cancelled (stop() called) or no network — either way, back off.
                if (running) sleepBeforeRetry();
            }
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static String isoNow() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        return fmt.format(new Date());
    }
}
