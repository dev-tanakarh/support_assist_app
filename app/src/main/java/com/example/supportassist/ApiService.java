package com.example.supportassist;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("signup")
    Call<ApiResponse<LoginResponse>> signup(@Body Map<String, String> body);

    @POST("login")
    Call<ApiResponse<LoginResponse>> login(@Body Map<String, String> body);

    @POST("refresh")
    Call<ApiResponse<LoginResponse>> refresh(@Body Map<String, String> body);

    @GET("profile")
    Call<ApiResponse<User>> getProfile();

    @PATCH("profile")
    Call<ApiResponse<User>> updateProfile(@Body Map<String, String> body);

    @GET("categories")
    Call<ApiResponse<List<Category>>> getCategories();

    @GET("tickets")
    Call<ApiResponse<TicketsResponse>> getTickets(
            @Query("status") String status,
            @Query("priority") String priority,
            @Query("category_id") Integer categoryId,
            @Query("q") String search,
            @Query("page") Integer page,
            @Query("per_page") Integer perPage
    );

    @GET("tickets/recent")
    Call<ApiResponse<List<Ticket>>> getRecentTickets(@Query("limit") Integer limit);

    @GET("tickets/{id}")
    Call<ApiResponse<Ticket>> getTicketDetails(@Path("id") String id);

    @POST("tickets")
    Call<ApiResponse<Ticket>> createTicket(@Body Map<String, Object> body);

    @Multipart
    @POST("tickets")
    Call<ApiResponse<Ticket>> createTicketMultipart(
            @Part("subject") RequestBody subject,
            @Part("description") RequestBody description,
            @Part("priority") RequestBody priority,
            @Part("category_id") RequestBody categoryId,
            @Part MultipartBody.Part attachment
    );

    @POST("tickets/{id}/review")
    Call<ApiResponse<Map<String, Object>>> submitReview(@Path("id") String id, @Body Map<String, Object> body);

    @GET("alerts")
    Call<ApiResponse<List<Alert>>> getAlerts(
            @Query("page") Integer page,
            @Query("per_page") Integer perPage
    );

    @PATCH("alerts/{id}/read")
    Call<ApiResponse<Map<String, String>>> markAlertRead(@Path("id") int id);
}