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
import java.io.IOException;

public class ApiClient {

    private static final String BASE_URL = "http://192.168.18.10:8080/api/";

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
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS) // NOTE: the long-poll call overrides this per-request, see below
                    .addInterceptor(logging)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            String token = tokenManager.getAccessToken();

                            Request.Builder requestBuilder = original.newBuilder()
                                    .header("Accept", "application/json");


                            if (original.header("Content-Type") == null) {
                                requestBuilder.header("Content-Type", "application/json");
                            }

                            if (token != null && !token.isEmpty()) {
                                requestBuilder.header("Authorization", "Bearer " + token);
                            }

                            return chain.proceed(requestBuilder.build());
                        }
                    })

                    .readTimeout(30, TimeUnit.SECONDS)
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
