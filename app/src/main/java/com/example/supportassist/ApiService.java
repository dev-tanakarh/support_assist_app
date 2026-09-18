package com.example.supportassist;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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

    @POST("forgot-password")
    Call<ApiResponse<Void>> forgotPassword(@Body Map<String, String> body);

    @POST("reset-password")
    Call<ApiResponse<Void>> resetPassword(@Body Map<String, String> body);

    @POST("user/fcm-token")
    Call<ApiResponse<Void>> updateFcmToken(@Body Map<String, String> body);

    @GET("profile")
    Call<ApiResponse<User>> getProfile();

    @GET("categories")
    Call<ApiResponse<List<Category>>> getCategories();

    @GET("tickets")
    Single<ApiResponse<TicketsResponse>> getTickets(
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

    /**
     * POST /api/tickets — must be multipart, not JSON, because `attachment` is a
     * real file part. Every text field still has to be sent as its own @Part
     * (a RequestBody with no filename) rather than bundled into a Map — Retrofit
     * has no "@Body inside @Multipart" option. @attachment is nullable: omit the
     * part entirely (don't pass an empty MultipartBody.Part) when there's no image.
     */
    @Multipart
    @POST("tickets")
    Call<ApiResponse<Ticket>> createTicket(
            @Part("subject") RequestBody subject,
            @Part("description") RequestBody description,
            @Part("category_id") RequestBody categoryId,
            @Part("priority") RequestBody priority,
            @Part("department") RequestBody department,
            @Part("device_type") RequestBody deviceType,
            @Part("room") RequestBody room,
            @Part MultipartBody.Part attachment
    );

    @POST("tickets/{id}/review")
    Call<ApiResponse<Map<String, Object>>> submitReview(@Path("id") String id, @Body Map<String, Object> body);

    @GET("alerts")
    Call<ApiResponse<List<Alert>>> getAlerts(
            @Query("page") Integer page,
            @Query("per_page") Integer perPage
    );

    @POST("alerts/{id}/read")
    Call<ApiResponse<Map<String, String>>> markAlertRead(@Path("id") int id);

    /**
     * Long-poll — see RUNNING.md on the backend for how this behaves. The call
     * itself just blocks for up to `timeout` seconds; nothing special needed on
     * the client beyond a generous Retrofit/OkHttp read timeout (see ApiClient).
     */
    @GET("alerts/poll")
    Call<ApiResponse<AlertPollResponse>> pollAlerts(
            @Query("since") String since,
            @Query("timeout") Integer timeout
    );

    // ---- Technician / Admin — added for role-based screens. Deliberately a
    // separate method from getTickets() above rather than adding params to
    // it: getTickets() feeds Paging3's RxPagingSource and changing its
    // signature risks breaking that working flow for no benefit, since these
    // callers don't need paging (technician/admin lists are small and just
    // use per_page directly, matching how the web app already does it). ----

    /**
     * `unassigned=1` -> the unassigned pool (technician/admin only, backend-enforced).
     * `unassigned=null` -> tickets scoped to the caller's role automatically:
     * technician sees their own assigned tickets, admin sees everything.
     * Same role-scoping the web app relies on — see routes.php's comment on GET /tickets.
     */
    @GET("tickets")
    Call<ApiResponse<TicketsResponse>> getTicketsList(
            @Query("status") String status,
            @Query("priority") String priority,
            @Query("unassigned") Integer unassigned,
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

    /** {"isActive": true|false}. Admin-only, and the backend refuses to touch ADMIN accounts here — see AdminController::setUserStatus. */
    @PATCH("admin/users/{id}/status")
    Call<ApiResponse<Void>> setUserStatus(@Path("id") String id, @Body Map<String, Object> body);

    /** Broadcasts to all END_USERs when `userId` is omitted from the body — see AdminController::sendAlert. */
    @POST("admin/alerts")
    Call<ApiResponse<Void>> sendAdminAlert(@Body Map<String, Object> body);
}
