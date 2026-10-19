package com.example.supportassist;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import io.reactivex.rxjava3.core.Single;

public interface ApiService {

    @POST("signup")
    Call<ApiResponse<LoginResponse>> signup(@Body Map<String, String> body);

    @POST("login")
    Call<ApiResponse<LoginResponse>> login(@Body Map<String, String> body);

    @POST("refresh")
    Call<ApiResponse<LoginResponse>> refresh(@Body Map<String, String> body);

    @POST("logout")
    Call<ApiResponse<Void>> logout();

    @POST("forgot-password")
    Call<ApiResponse<Void>> forgotPassword(@Body Map<String, String> body);

    @POST("reset-password")
    Call<ApiResponse<Void>> resetPassword(@Body Map<String, String> body);

    @POST("user/fcm-token")
    Call<ApiResponse<Void>> updateFcmToken(@Body Map<String, String> body);

    @GET("profile")
    Call<ApiResponse<User>> getProfile();

    @PATCH("profile")
    Call<ApiResponse<User>> updateProfile(@Body Map<String, String> body);

    @Multipart
    @POST("profile/avatar")
    Call<ApiResponse<User>> updateProfilePicture(@Part MultipartBody.Part avatar);

    @GET("categories")
    Call<ApiResponse<List<Category>>> getCategories();

    @GET("tickets")
    Single<ApiResponse<TicketsResponse>> getTickets(
            @Query("status") String status,
            @Query("priority") String priority,
            @Query("category_id") Integer categoryId,
            @Query("q") String search,
            @Query("unassigned") Boolean unassigned,
            @Query("resolved_after") String resolvedAfter,
            @Query("page") Integer page,
            @Query("per_page") Integer perPage
    );

    @GET("tickets/recent")
    Call<ApiResponse<List<Ticket>>> getRecentTickets(@Query("limit") Integer limit);

    /**
     * @param unassigned Set to 1 if fetching a ticket from the unassigned pool.
     */
    @GET("tickets/{id}")
    Call<ApiResponse<Ticket>> getTicketDetails(
            @Path("id") String id,
            @Query("unassigned") Integer unassigned
    );

    @GET("tickets/{id}/messages")
    Call<ApiResponse<List<Message>>> getTicketMessages(@Path("id") String id);

    @POST("tickets/{id}/messages")
    Call<ApiResponse<Message>> postTicketMessage(@Path("id") String id, @Body Map<String, Object> body);

    @GET("tickets/{id}/attachment")
    Call<ResponseBody> getTicketAttachment(
            @Path("id") String id,
            @Query("variant") String variant
    );

    /**
     * Create ticket with attachment using multipart.
     */
    @Multipart
    @POST("tickets")
    Call<ApiResponse<Ticket>> createTicketMultipart(
            @Part("subject") RequestBody subject,
            @Part("description") RequestBody description,
            @Part("category_id") RequestBody categoryId,
            @Part("priority") RequestBody priority,
            @Part("department") RequestBody department,
            @Part("device_type") RequestBody deviceType,
            @Part("room") RequestBody room,
            @Part MultipartBody.Part attachment
    );

    /**
     * Create ticket without attachment using JSON (Stage 2 Requirement).
     */
    @POST("tickets")
    Call<ApiResponse<Ticket>> createTicketJson(@Body Map<String, Object> body);

    @POST("tickets/{id}/review")
    Call<ApiResponse<Map<String, Object>>> submitReview(@Path("id") String id, @Body Map<String, Object> body);

    @GET("alerts")
    Call<ApiResponse<List<Alert>>> getAlerts(
            @Query("page") Integer page,
            @Query("per_page") Integer perPage
    );

    @PATCH("alerts/{id}/read")
    Call<ApiResponse<Map<String, String>>> markAlertRead(@Path("id") String id);

    @GET("alerts/poll")
    Call<ApiResponse<AlertPollResponse>> pollAlerts(
            @Query("since") String since,
            @Query("timeout") Integer timeout
    );

    @GET("tickets")
    Call<ApiResponse<TicketsResponse>> getTicketsList(
            @Query("status") String status,
            @Query("priority") String priority,
            @Query("unassigned") Boolean unassigned,
            @Query("page") Integer page,
            @Query("per_page") Integer perPage
    );

    @POST("tickets/{id}/accept")
    Call<ApiResponse<Ticket>> acceptTicket(@Path("id") String id);

    @PATCH("tickets/{id}/status")
    Call<ApiResponse<Ticket>> updateTicketStatus(@Path("id") String id, @Body Map<String, Object> body);

    @POST("tickets/{id}/assign")
    Call<ApiResponse<Ticket>> assignTicket(@Path("id") String id, @Body Map<String, String> body);

    @GET("admin/dashboard")
    Call<ApiResponse<DashboardStats>> getDashboard();

    @GET("admin/users")
    Call<ApiResponse<List<User>>> getUsers(@Query("type") String type);

    @POST("admin/technicians")
    Call<ApiResponse<User>> createTechnician(@Body Map<String, String> body);

    @DELETE("admin/technicians/{id}")
    Call<ApiResponse<Void>> deleteTechnician(@Path("id") String id);

    @PATCH("admin/users/{id}/status")
    Call<ApiResponse<Void>> setUserStatus(@Path("id") String id, @Body Map<String, Object> body);

    @POST("admin/alerts")
    Call<ApiResponse<Void>> sendAdminAlert(@Body Map<String, Object> body);
}
