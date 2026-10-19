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
 * Stage 7: Notifications & Live Updates.
 * Implements long-polling against /api/alerts/poll.
 */
@Singleton
public class AlertPoller {
    private static final String TAG = "AlertPoller";
    private static final int TIMEOUT_SECONDS = 25;
    private static final long RETRY_DELAY_MS = 5_000;

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
        // Start from now if we don't have a cursor
        if (since == null) since = isoNow();
        new Thread(this::loop, "alert-poller").start();
    }

    public synchronized void stop() {
        running = false;
        if (currentCall != null) {
            currentCall.cancel();
        }
    }

    private void loop() {
        while (running) {
            Call<ApiResponse<AlertPollResponse>> call = apiService.pollAlerts(since, TIMEOUT_SECONDS);
            currentCall = call;
            try {
                Response<ApiResponse<AlertPollResponse>> response = call.execute();
                if (!running) break;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AlertPollResponse data = response.body().getData();
                    
                    // Stage 7 requirement: Use returned serverTime as the next cursor
                    if (data.getServerTime() != null) {
                        since = data.getServerTime();
                    }

                    if (data.getAlerts() != null && !data.getAlerts().isEmpty()) {
                        Log.d(TAG, "New alerts received: " + data.getAlerts().size());
                        // Refresh cached data to reflect changes (Stage 5/8)
                        DataRepository.getInstance(appContext).syncDataSilently();
                    }
                } else if (response.code() != 404) {
                    sleepBeforeRetry();
                }
            } catch (java.io.IOException e) {
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
