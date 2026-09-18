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
    // Set in app/build.gradle.kts from local.properties (gitignored) — see the
    // comment there. Defaults to the Android emulator's loopback alias, which
    // matches the backend's docker-compose port out of the box.
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

            // Certificate pinning was here before, but with a placeholder hash
            // that could never match a real certificate — it would have broken
            // every single request the moment BASE_URL pointed at a real server.
            // Deliberately left out: this app's threat model (a school/internal
            // helpdesk tool, not a target for anyone running a MITM attack) doesn't
            // justify it, and a pin that goes stale when a cert rotates silently
            // breaks the whole app for every user until someone ships a fix. Plain
            // HTTPS is the right call here. If that judgment ever changes, get the
            // real pin with:
            //   openssl s_client -connect your-host:443 | openssl x509 -pubkey -noout \
            //     | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64

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

                            // Don't force Content-Type here — multipart requests
                            // (ticket creation with an attachment) set their own
                            // "multipart/form-data; boundary=..." header, and
                            // .header(...) REPLACES rather than adds, so forcing
                            // "application/json" on every request would silently
                            // corrupt every multipart upload's Content-Type.
                            if (original.header("Content-Type") == null) {
                                requestBuilder.header("Content-Type", "application/json");
                            }

                            if (token != null && !token.isEmpty()) {
                                requestBuilder.header("Authorization", "Bearer " + token);
                            }

                            return chain.proceed(requestBuilder.build());
                        }
                    })
                    // The long-poll endpoint (GET /api/alerts/poll) can legitimately
                    // block for ~25s server-side — give it real headroom rather than
                    // the 15s default above, or OkHttp will time it out as if it failed.
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    // Needed specifically for ApiService.getTickets(), which returns
                    // Single<...> for Paging3's RxPagingSource — every other method
                    // returns plain Call<...>, which Retrofit handles natively
                    // without needing an adapter factory at all, so this addition
                    // doesn't change anything for the rest of the app.
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
