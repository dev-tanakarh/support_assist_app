package com.example.supportassist;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

public class ApiClient {

    private static final String TAG = "ApiClient";
    
    // Centralized configuration using BuildConfig (Stage 1)
    private static final String BASE_URL = "http://192.168.18.10:8080/api/";
    private static final Object lock = new Object();
    private static Retrofit retrofit = null;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static String getWebSocketUrl() {
        // Dev requirement: http://192.168.18.10:8080/api/ -> ws://192.168.18.10:8081
        String wsUrl = BASE_URL;
        if (wsUrl.startsWith("https://")) {
            wsUrl = wsUrl.replaceFirst("https://", "wss://");
        } else if (wsUrl.startsWith("http://")) {
            wsUrl = wsUrl.replaceFirst("http://", "ws://");
        }
        
        // Replace port and path for dev environment as per spec
        return wsUrl.replace(":8080/api/", ":8081");
    }

    /**
     * Resolves a relative URL to a full URL using the backend root URL.
     */
    public static String getFullImageUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) return null;
        if (relativeUrl.startsWith("http")) return relativeUrl;
        
        String rootUrl = BASE_URL;
        if (rootUrl.endsWith("/api/")) {
            rootUrl = rootUrl.substring(0, rootUrl.length() - 5);
        } else if (rootUrl.endsWith("/api")) {
            rootUrl = rootUrl.substring(0, rootUrl.length() - 4);
        }
        
        String cleanRelative = relativeUrl.startsWith("/") ? relativeUrl.substring(1) : relativeUrl;
        return rootUrl.endsWith("/") ? rootUrl + cleanRelative : rootUrl + "/" + cleanRelative;
    }

    /**
     * Creates a GlideUrl with the necessary Authorization header.
     */
    public static GlideUrl getAuthenticatedGlideUrl(String relativeUrl, String token) {
        String fullUrl = getFullImageUrl(relativeUrl);
        if (fullUrl == null) return null;
        return new GlideUrl(fullUrl, new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + token)
                .build());
    }

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            Log.d(TAG, "Initializing Retrofit with BASE_URL: " + BASE_URL);
            
            final Context appContext = context.getApplicationContext();
            final TokenManager tokenManager = new TokenManager(appContext);

            // Redact sensitive headers for security (Stage 1)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d("OkHttp", message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            logging.redactHeader("Authorization");

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            String token = tokenManager.getAccessToken();

                            Request.Builder requestBuilder = original.newBuilder()
                                    .header("Accept", "application/json")
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .header("User-Agent", "SupportAssist-Android-App");

                            if (token != null && !token.isEmpty() && !token.equalsIgnoreCase("null")) {
                                requestBuilder.header("Authorization", "Bearer " + token.trim());
                            }

                            Response response = chain.proceed(requestBuilder.build());

                            // Stage 2: Handle 401 Unauthorized - Attempt Token Refresh
                            if (response.code() == 401 && !original.url().encodedPath().contains("/login") 
                                    && !original.url().encodedPath().contains("/refresh")) {
                                
                                synchronized (lock) {
                                    String currentToken = tokenManager.getAccessToken();
                                    
                                    // If another thread refreshed the token already, retry immediately
                                    if (currentToken != null && !currentToken.equals(token)) {
                                        response.close();
                                        return chain.proceed(original.newBuilder()
                                                .header("Authorization", "Bearer " + currentToken.trim())
                                                .build());
                                    }

                                    String refreshToken = tokenManager.getRefreshToken();
                                    if (refreshToken == null || refreshToken.isEmpty()) {
                                        handleSessionExpiry(appContext, tokenManager);
                                        return response;
                                    }

                                    Log.d(TAG, "Access token expired. Attempting rotation...");
                                    ApiResponse<LoginResponse> refreshResult = performRefresh(refreshToken);
                                    
                                    if (refreshResult != null && refreshResult.isSuccess()) {
                                        LoginResponse data = refreshResult.getData();
                                        tokenManager.saveTokens(data.getAccessToken(), data.getRefreshToken());
                                        Log.d(TAG, "Token rotated successfully.");
                                        
                                        // Real-time Update: Reconnect WebSocket with the new token
                                        WebSocketManager.getInstance().connect(data.getAccessToken());

                                        response.close();
                                        return chain.proceed(original.newBuilder()
                                                .header("Authorization", "Bearer " + data.getAccessToken().trim())
                                                .build());
                                    } else {
                                        Log.e(TAG, "Refresh token invalid. Revoking session.");
                                        handleSessionExpiry(appContext, tokenManager);
                                        return response;
                                    }
                                }
                            }

                            return response;
                        }
                    })
                    .addInterceptor(logging) // Logging last to capture final state
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    private static ApiResponse<LoginResponse> performRefresh(String refreshToken) {
        // Separate Retrofit instance for refresh to avoid interceptor recursion
        Retrofit refreshRetrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService authService = refreshRetrofit.create(ApiService.class);
        
        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);
        
        try {
            retrofit2.Response<ApiResponse<LoginResponse>> refreshResponse = authService.refresh(body).execute();
            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                return refreshResponse.body();
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error during token refresh", e);
        }
        return null;
    }

    private static void handleSessionExpiry(Context context, TokenManager tokenManager) {
        tokenManager.clear();
        WebSocketManager.getInstance().disconnect();
        DataRepository.getInstance(context).clearCache(context);
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
