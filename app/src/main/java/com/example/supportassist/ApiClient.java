package com.example.supportassist;

import android.content.Context;
import android.util.Log;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;

public class ApiClient {
    // IMPORTANT: Ensure this matches your laptop's IP address or ngrok URL.
    // The trailing slash is mandatory for Retrofit.
    private static final String BASE_URL = "http://192.168.18.7:8080/api/";
    private static Retrofit retrofit = null;

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            Log.d("ApiClient", "Initializing Retrofit with BASE_URL: " + BASE_URL);
            
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            final TokenManager tokenManager = new TokenManager(context.getApplicationContext());

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            String token = tokenManager.getAccessToken();

                            Request.Builder requestBuilder = original.newBuilder()
                                    .header("Content-Type", "application/json")
                                    .header("Accept", "application/json");

                            if (token != null && !token.isEmpty()) {
                                requestBuilder.header("Authorization", "Bearer " + token);
                            }

                            return chain.proceed(requestBuilder.build());
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
