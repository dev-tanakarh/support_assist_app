package com.example.supportassist;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fetches the current FCM device token and registers it with the backend.
 * Call this once the user is authenticated (MainActivity.onCreate is the
 * single place it's called from — every entry into the app, whether via
 * fresh login, signup, or biometric auto-login, passes through there).
 *
 * Also called from MyFirebaseMessagingService.onNewToken(), since FCM can
 * rotate the token at any time (app reinstall, data cleared, token expiry)
 * — the backend needs to hear about that too, not just the first token.
 */
final class FcmTokenSync {
    private static final String TAG = "FcmTokenSync";

    private FcmTokenSync() { }

    static void registerCurrentToken(ApiService apiService) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                // Expected until google-services.json is configured with a real
                // Firebase project — see RUNNING.md. Not fatal, just no push yet.
                Log.w(TAG, "Could not get FCM token", task.getException());
                return;
            }
            send(apiService, task.getResult());
        });
    }

    static void send(ApiService apiService, String token) {
        if (token == null || token.isEmpty()) return;

        Map<String, String> body = new HashMap<>();
        body.put("fcm_token", token);
        apiService.updateFcmToken(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Backend rejected FCM token: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                // Fine to lose this one silently — it'll retry next app open,
                // and onNewToken() will fire again if the token itself changes.
                Log.w(TAG, "Failed to send FCM token", t);
            }
        });
    }
}
