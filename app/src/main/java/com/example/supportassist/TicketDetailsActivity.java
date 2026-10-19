package com.example.supportassist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.supportassist.viewmodel.TicketViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class TicketDetailsActivity extends AppCompatActivity implements WebSocketManager.MessageListener {

    private static final String TAG = "TicketDetails";
    
    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;
    @Inject WebSocketManager webSocketManager;

    private TextView tvId, tvSubject, tvStatus, tvPriority, tvDescription, tvDate, tvTechnician, tvDueDate;
    private TextView tvReporter, tvCategory, tvDepartment, tvRoom, tvDeviceType;
    private ImageView ivReporterAvatar, ivTechnicianAvatar;
    private RatingBar rbTechnicianRating;
    private LinearLayout llUpdatesContainer;
    
    private RecyclerView rvMessages;
    private MessageAdapter messageAdapter;
    private TextView tvEmptyMessages;
    private ConstraintLayout llMessageInput;
    private EditText etMessage;
    private ImageButton btnSendMessage;
    private CheckBox cbInternalNote;

    private RatingBar rbRating;
    private EditText etComment;
    private ImageButton btnSendComment;
    private ConstraintLayout llCommentInput;
    
    private MaterialCardView cvAttachment;
    private ImageView ivAttachment;
    private ProgressBar progressBar;

    private LinearLayout llRoleActions, llResolveBlock, llAssignBlock;
    private MaterialButton btnAccept, btnResolve, btnAssign;
    private EditText etResolveNote;
    private Spinner spAssignTechnician;
    private List<User> assignableTechnicians = new ArrayList<>();

    private String ticketId;
    private boolean isUnassigned;
    private Ticket currentTicket;
    private TicketViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_details);

        viewModel = new ViewModelProvider(this).get(TicketViewModel.class);

        initViews();
        setupRecyclerView();

        ticketId = getIntent().getStringExtra("ticket_id");
        isUnassigned = getIntent().getBooleanExtra("is_unassigned", false);

        if (ticketId != null) {
            observeTicketData();
            observeMessages();
            
            viewModel.refreshTicketDetail(ticketId, isUnassigned ? 1 : null);
            viewModel.refreshMessages(ticketId);
            
            // Subscribe to real-time updates
            webSocketManager.addListener(this);
            webSocketManager.subscribe(ticketId);
        } else {
            Toast.makeText(this, "Error: Ticket ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSendMessage.setOnClickListener(v -> postMessage());
        btnSendComment.setOnClickListener(v -> submitReview());
        
        btnAccept.setOnClickListener(v -> acceptTicket());
        btnResolve.setOnClickListener(v -> resolveTicket());
        btnAssign.setOnClickListener(v -> assignTicket());
        
        cvAttachment.setOnClickListener(v -> {
            if (currentTicket != null) {
                String fullUrl = ApiClient.getBaseUrl() + "tickets/" + ticketId + "/attachment?variant=full";
                Intent intent = new Intent(this, ImageViewerActivity.class);
                intent.putExtra("image_url", fullUrl);
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void observeTicketData() {
        disposables.add(viewModel.getTicketDetails(ticketId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ticket -> {
                    if (ticket != null) {
                        updateUI(ticket);
                    }
                }, throwable -> Log.e(TAG, "Error observing ticket", throwable)));
    }

    private void observeMessages() {
        disposables.add(viewModel.getTicketMessages(ticketId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(messages -> {
                    messageAdapter.setMessages(messages != null ? messages : new ArrayList<>());
                    tvEmptyMessages.setVisibility(messages == null || messages.isEmpty() ? View.VISIBLE : View.GONE);
                    if (messages != null && !messages.isEmpty()) {
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                }, throwable -> Log.e(TAG, "Error observing messages", throwable)));
    }

    @Override
    protected void onDestroy() {
        if (ticketId != null) {
            webSocketManager.unsubscribe(ticketId);
            webSocketManager.removeListener(this);
        }
        disposables.dispose();
        super.onDestroy();
    }

    private void initViews() {
        tvId = findViewById(R.id.tv_ticket_id);
        tvSubject = findViewById(R.id.tv_subject);
        tvStatus = findViewById(R.id.tv_status_tag);
        tvPriority = findViewById(R.id.tv_priority_tag);
        tvDescription = findViewById(R.id.tv_description);
        tvDate = findViewById(R.id.tv_submitted_date);
        tvDueDate = findViewById(R.id.tv_due_date);
        tvTechnician = findViewById(R.id.tv_assigned_technician);
        tvReporter = findViewById(R.id.tv_reporter);
        tvCategory = findViewById(R.id.tv_category);
        tvDepartment = findViewById(R.id.tv_department);
        tvRoom = findViewById(R.id.tv_room);
        tvDeviceType = findViewById(R.id.tv_device_type);
        ivReporterAvatar = findViewById(R.id.iv_reporter_avatar);
        ivTechnicianAvatar = findViewById(R.id.iv_technician_avatar);
        rbTechnicianRating = findViewById(R.id.rb_technician_profile_rating);
        llUpdatesContainer = findViewById(R.id.ll_updates_container);

        rvMessages = findViewById(R.id.rv_messages);
        tvEmptyMessages = findViewById(R.id.tv_empty_messages);
        llMessageInput = findViewById(R.id.ll_message_input);
        etMessage = findViewById(R.id.et_message);
        btnSendMessage = findViewById(R.id.btn_send_message);
        cbInternalNote = findViewById(R.id.cb_internal_note);

        etComment = findViewById(R.id.et_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
        rbRating = findViewById(R.id.rb_rating);
        llCommentInput = findViewById(R.id.ll_comment_input);
        cvAttachment = findViewById(R.id.cv_attachment);
        ivAttachment = findViewById(R.id.iv_attachment);
        progressBar = findViewById(R.id.loading_indicator);

        llRoleActions = findViewById(R.id.ll_role_actions);
        btnAccept = findViewById(R.id.btn_accept);
        llResolveBlock = findViewById(R.id.ll_resolve_block);
        etResolveNote = findViewById(R.id.et_resolve_note);
        btnResolve = findViewById(R.id.btn_resolve);
        llAssignBlock = findViewById(R.id.ll_assign_block);
        spAssignTechnician = findViewById(R.id.sp_assign_technician);
        btnAssign = findViewById(R.id.btn_assign);
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(tokenManager.getUserId(), tokenManager);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void postMessage() {
        String bodyText = etMessage.getText().toString().trim();
        if (bodyText.isEmpty()) return;

        btnSendMessage.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("body", bodyText);
        body.put("isInternal", cbInternalNote.isChecked());

        apiService.postTicketMessage(ticketId, body).enqueue(new Callback<ApiResponse<Message>>() {
            @Override
            public void onResponse(Call<ApiResponse<Message>> call, Response<ApiResponse<Message>> response) {
                btnSendMessage.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    etMessage.setText("");
                    // No need to manually add message, Repository/Room will pick up real-time update or manual sync
                    viewModel.refreshMessages(ticketId);
                } else {
                    String error = ApiErrors.extractMessage(response, "Failed to send message");
                    Toast.makeText(TicketDetailsActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<Message>> call, Throwable t) {
                btnSendMessage.setEnabled(true);
                Toast.makeText(TicketDetailsActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMessageCreated(String tId, Message message) {
        if (message == null || (tId != null && !tId.equals(ticketId))) return;
        
        // Safety check for internal notes visibility
        if (message.isInternal() && !tokenManager.isTechnician() && !tokenManager.isAdmin()) {
            return;
        }

        // We use repository sync for stability, but could add manually for extra speed
        runOnUiThread(() -> viewModel.refreshMessages(ticketId));
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        Log.d(TAG, "WS Connection: " + (connected ? "Connected" : "Disconnected"));
    }

    private void updateUI(Ticket ticket) {
        if (ticket == null) return;
        currentTicket = ticket;

        tvId.setText(ticket.getId());
        tvSubject.setText(ticket.getSubject());
        tvDescription.setText(ticket.getDescription());
        tvDate.setText(ticket.getTimeAgo());
        
        tvCategory.setText(getFieldValue(ticket.getCategory() != null ? ticket.getCategory().getName() : null));
        tvDepartment.setText(getFieldValue(ticket.getDepartment()));
        tvRoom.setText(getFieldValue(ticket.getRoom()));
        tvDeviceType.setText(getFieldValue(ticket.getDeviceType()));
        updatePriorityUI(ticket.getPriority());

        if (ticket.getDueAt() != null) {
            tvDueDate.setVisibility(View.VISIBLE);
            tvDueDate.setText(getString(R.string.due_date_format, ticket.getDueAt()));
            tvDueDate.setTextColor(ticket.isOverdue() ? ContextCompat.getColor(this, R.color.priority_high_text) : ContextCompat.getColor(this, R.color.text_grey));
        } else {
            tvDueDate.setVisibility(View.GONE);
        }

        String token = tokenManager.getAccessToken();

        // Load Reporter
        if (ticket.getReporter() != null) {
            tvReporter.setText(ticket.getReporter().getName());
            String avatarPath = ticket.getReporter().getAvatarThumbnailUrl();
            if (avatarPath == null || avatarPath.isEmpty()) avatarPath = ticket.getReporter().getProfileImageUrl();
            
            GlideUrl avatarUrl = ApiClient.getAuthenticatedGlideUrl(avatarPath, token);
            Glide.with(this)
                    .load(avatarUrl != null ? avatarUrl : R.drawable.ic_profile)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivReporterAvatar);
        }

        // Secure Attachment (Thumbnail)
        if (ticket.getAttachmentUrl() != null && !ticket.getAttachmentUrl().isEmpty()) {
            cvAttachment.setVisibility(View.VISIBLE);
            String secureThumbUrl = ApiClient.getBaseUrl() + "tickets/" + ticket.getId() + "/attachment?variant=thumbnail";
            GlideUrl glideUrl = new GlideUrl(secureThumbUrl, new LazyHeaders.Builder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build());
            Glide.with(this)
                    .load(glideUrl)
                    .placeholder(R.color.card_bg)
                    .error(R.drawable.ic_logo)
                    .into(ivAttachment);
        } else {
            cvAttachment.setVisibility(View.GONE);
        }

        // Load Technician
        if (ticket.getAssignedTechnician() != null && ticket.getAssignedTechnician().getName() != null) {
            User tech = ticket.getAssignedTechnician();
            tvTechnician.setText(tech.getName());
            String avatarPath = tech.getAvatarThumbnailUrl();
            if (avatarPath == null || avatarPath.isEmpty()) avatarPath = tech.getProfileImageUrl();
            
            GlideUrl avatarUrl = ApiClient.getAuthenticatedGlideUrl(avatarPath, token);
            Glide.with(this)
                    .load(avatarUrl != null ? avatarUrl : R.drawable.ic_profile)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(ivTechnicianAvatar);
            
            if (tech.getRating() != null) {
                rbTechnicianRating.setRating(tech.getRating().floatValue());
                rbTechnicianRating.setVisibility(View.VISIBLE);
            }
        } else {
            tvTechnician.setText(R.string.not_yet_assigned);
            ivTechnicianAvatar.setImageResource(R.drawable.ic_profile);
            rbTechnicianRating.setVisibility(View.GONE);
        }

        updateStatusUI(ticket.getStatus());
        renderStatusHistory(ticket.getStatusHistory());
        applyRoleActions(ticket);
    }

    private String getFieldValue(String value) {
        if (value == null || value.trim().isEmpty()) return "N/A";
        return value;
    }

    private void updatePriorityUI(String priority) {
        tvPriority.setText(priority != null ? priority : "Low");
        int bg = R.drawable.priority_low_bg, text = R.color.priority_low_text;
        if (priority != null) {
            if (priority.equalsIgnoreCase("HIGH")) { 
                bg = R.drawable.priority_high_bg; 
                text = R.color.priority_high_text; 
            } else if (priority.equalsIgnoreCase("MEDIUM")) { 
                bg = R.drawable.priority_medium_bg; 
                text = R.color.priority_medium_text; 
            }
        }
        tvPriority.setBackgroundResource(bg);
        tvPriority.setTextColor(ContextCompat.getColor(this, text));
    }

    private void updateStatusUI(String status) {
        tvStatus.setText(statusLabel(status));
        if (status == null) return;
        int bg = R.drawable.status_open_bg, text = R.color.status_open_text;
        if (status.equalsIgnoreCase("IN_PROGRESS")) { bg = R.drawable.status_progress_bg; text = R.color.status_progress_text; }
        else if (status.equalsIgnoreCase("RESOLVED")) { bg = R.drawable.status_resolved_bg; text = R.color.status_resolved_text; }
        else if (status.equalsIgnoreCase("CLOSED")) { bg = R.drawable.status_closed_bg; text = R.color.white; }
        
        tvStatus.setBackgroundResource(bg);
        tvStatus.setTextColor(ContextCompat.getColor(this, text));
    }

    private void renderStatusHistory(List<Ticket.StatusHistory> history) {
        llUpdatesContainer.removeAllViews();
        if (history == null || history.isEmpty()) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Ticket.StatusHistory item : history) {
            View updateView = inflater.inflate(R.layout.item_update, llUpdatesContainer, false);
            ((TextView)updateView.findViewById(R.id.tv_admin_name)).setText(String.format("%s → %s", statusLabel(item.getOldStatus()), statusLabel(item.getNewStatus())));
            ((TextView)updateView.findViewById(R.id.tv_update_time)).setText(item.getChangedAt());
            ((TextView)updateView.findViewById(R.id.tv_update_note)).setText(item.getNote() != null ? item.getNote() : "No note provided.");
            llUpdatesContainer.addView(updateView);
        }
    }

    private void applyRoleActions(Ticket ticket) {
        String role = tokenManager.getUserType();
        boolean isStaff = tokenManager.isTechnician() || tokenManager.isAdmin();
        boolean isResolvedOrClosed = "RESOLVED".equalsIgnoreCase(ticket.getStatus()) || "CLOSED".equalsIgnoreCase(ticket.getStatus());
        
        llMessageInput.setVisibility(View.VISIBLE);
        cbInternalNote.setVisibility(isStaff ? View.VISIBLE : View.GONE);
        
        boolean assignedToMe = ticket.getAssignedTechnician() != null && 
                             ticket.getAssignedTechnician().getId() != null && 
                             ticket.getAssignedTechnician().getId().equals(tokenManager.getUserId());

        if (isStaff && ticket.isUnassigned()) {
            llMessageInput.setVisibility(View.GONE); 
        } else if (tokenManager.isTechnician() && !assignedToMe && !tokenManager.isAdmin()) {
            llMessageInput.setVisibility(View.GONE); 
        } else if (!isStaff && isResolvedOrClosed) {
            llMessageInput.setVisibility(View.GONE);
        }

        llCommentInput.setVisibility(!isStaff && "RESOLVED".equalsIgnoreCase(ticket.getStatus()) ? View.VISIBLE : View.GONE);

        btnAccept.setVisibility(isStaff && ticket.isUnassigned() ? View.VISIBLE : View.GONE);
        llResolveBlock.setVisibility(isStaff && assignedToMe && !isResolvedOrClosed ? View.VISIBLE : View.GONE);
        llAssignBlock.setVisibility(tokenManager.isAdmin() && ticket.isUnassigned() ? View.VISIBLE : View.GONE);
        
        checkRoleActionsVisibility();
        
        if (llAssignBlock.getVisibility() == View.VISIBLE) loadTechniciansForAssign();
    }

    private void checkRoleActionsVisibility() {
        boolean anyVisible = btnAccept.getVisibility() == View.VISIBLE || 
                            llResolveBlock.getVisibility() == View.VISIBLE || 
                            llAssignBlock.getVisibility() == View.VISIBLE;
        llRoleActions.setVisibility(anyVisible ? View.VISIBLE : View.GONE);
    }

    private void acceptTicket() {
        btnAccept.setEnabled(false);
        apiService.acceptTicket(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(TicketDetailsActivity.this, "Ticket claimed", Toast.LENGTH_SHORT).show();
                    btnAccept.setVisibility(View.GONE);
                    checkRoleActionsVisibility();
                    viewModel.refreshTicketDetail(ticketId, null);
                } else {
                    btnAccept.setEnabled(true);
                    String error = ApiErrors.extractMessage(response, "Could not claim ticket");
                    Toast.makeText(TicketDetailsActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) { 
                btnAccept.setEnabled(true);
                Toast.makeText(TicketDetailsActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resolveTicket() {
        btnResolve.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("status", "RESOLVED");
        String note = etResolveNote.getText().toString().trim();
        if (!note.isEmpty()) body.put("note", note);

        apiService.updateTicketStatus(ticketId, body).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(TicketDetailsActivity.this, "Ticket resolved", Toast.LENGTH_SHORT).show();
                    etResolveNote.setText("");
                    llResolveBlock.setVisibility(View.GONE);
                    checkRoleActionsVisibility();
                    viewModel.refreshTicketDetail(ticketId, null);
                } else {
                    btnResolve.setEnabled(true);
                    String error = ApiErrors.extractMessage(response, "Could not resolve ticket");
                    Toast.makeText(TicketDetailsActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) { 
                btnResolve.setEnabled(true);
                Toast.makeText(TicketDetailsActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTechniciansForAssign() {
        apiService.getUsers("TECHNICIAN").enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                if (response.body() == null || !response.body().isSuccess()) return;
                assignableTechnicians = response.body().getData();
                if (assignableTechnicians == null) assignableTechnicians = new ArrayList<>();

                List<String> names = new ArrayList<>();
                for (User t : assignableTechnicians) names.add(t.getName());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(TicketDetailsActivity.this,
                        android.R.layout.simple_spinner_item, names);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spAssignTechnician.setAdapter(adapter);
            }
            @Override public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {}
        });
    }

    private void assignTicket() {
        int pos = spAssignTechnician.getSelectedItemPosition();
        if (pos < 0 || pos >= assignableTechnicians.size()) return;
        String technicianId = assignableTechnicians.get(pos).getId();

        btnAssign.setEnabled(false);
        Map<String, String> body = new HashMap<>();
        body.put("technicianId", technicianId);

        apiService.assignTicket(ticketId, body).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(TicketDetailsActivity.this, "Ticket assigned", Toast.LENGTH_SHORT).show();
                    llAssignBlock.setVisibility(View.GONE);
                    checkRoleActionsVisibility();
                    viewModel.refreshTicketDetail(ticketId, null);
                } else {
                    btnAssign.setEnabled(true);
                    String error = ApiErrors.extractMessage(response, "Could not assign ticket");
                    Toast.makeText(TicketDetailsActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) { 
                btnAssign.setEnabled(true);
                Toast.makeText(TicketDetailsActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReview() {
        int rating = Math.round(rbRating.getRating());
        if (rating < 1) rating = 1;
        Map<String, Object> body = new HashMap<>();
        body.put("rating", rating);
        body.put("comment", etComment.getText().toString());

        apiService.submitReview(ticketId, body).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    etComment.setText("");
                    llCommentInput.setVisibility(View.GONE);
                    viewModel.refreshTicketDetail(ticketId, null);
                }
            }
            @Override public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private String statusLabel(String status) {
        if (status == null) return "Unknown";
        return status.replace('_', ' ');
    }
}
