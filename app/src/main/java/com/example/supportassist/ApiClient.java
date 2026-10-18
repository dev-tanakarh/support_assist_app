package com.example.supportassist;

import android.content.Context;
import android.util.Log;
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
import java.io.IOException;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "http://192.168.0.175:8080/api/";

    private static Retrofit retrofit = null;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    /**
     * Resolves a relative URL to a full URL using the backend root URL.
     * Handles both absolute and relative URLs.
     */
    public static String getFullImageUrl(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) return null;
        if (relativeUrl.startsWith("http")) return relativeUrl;
        
        // Remove /api/ suffix from BASE_URL to get the root host
        String rootUrl = BASE_URL;
        if (rootUrl.endsWith("/api/")) {
            rootUrl = rootUrl.substring(0, rootUrl.length() - 5);
        } else if (rootUrl.endsWith("/api")) {
            rootUrl = rootUrl.substring(0, rootUrl.length() - 4);
        }
        
        String cleanRelative = relativeUrl;
        if (cleanRelative.startsWith("/")) {
            cleanRelative = cleanRelative.substring(1);
        }
        
        if (rootUrl.endsWith("/")) {
            return rootUrl + cleanRelative;
        } else {
            return rootUrl + "/" + cleanRelative;
        }
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
            
            final TokenManager tokenManager = new TokenManager(context.getApplicationContext());

            // Use Log.d for consistency
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d("OkHttp", message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

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
                                String cleanToken = token.trim();
                                requestBuilder.header("Authorization", "Bearer " + cleanToken);
                                Log.d(TAG, "Auth Header Attached: Yes (" + original.method() + " " + original.url().encodedPath() + ")");
                            } else {
                                Log.w(TAG, "Auth Header Attached: No (Token is " + (token == null ? "null" : "empty") + ")");
                            }

                            return chain.proceed(requestBuilder.build());
                        }
                    })
                    .addInterceptor(logging) // Important: logging interceptor must be AFTER auth interceptor to log headers
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
}
