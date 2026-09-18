package com.example.supportassist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketDetailsActivity extends AppCompatActivity {

    private TextView tvId, tvSubject, tvStatus, tvPriority, tvDescription, tvDate, tvTechnician;
    private LinearLayout llUpdatesContainer;
    private EditText etComment;
    private ImageButton btnSendComment;
    private RatingBar rbRating;
    private LinearLayout llCommentInput;

    private LinearLayout llRoleActions, llResolveBlock, llAssignBlock;
    private MaterialButton btnAccept, btnResolve, btnAssign;
    private EditText etResolveNote;
    private Spinner spAssignTechnician;
    private List<User> assignableTechnicians = new ArrayList<>();

    private ApiService apiService;
    private DataRepository repository;
    private TokenManager tokenManager;
    private String ticketId;
    private Ticket currentTicket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_details);

        apiService = ApiClient.getApiService(this);
        repository = DataRepository.getInstance(this);
        tokenManager = new TokenManager(this);

        tvId = findViewById(R.id.tv_ticket_id);
        tvSubject = findViewById(R.id.tv_subject);
        tvStatus = findViewById(R.id.tv_status_tag);
        tvPriority = findViewById(R.id.tv_priority_tag);
        tvDescription = findViewById(R.id.tv_description);
        tvDate = findViewById(R.id.tv_submitted_date);
        tvTechnician = findViewById(R.id.tv_assigned_technician);
        llUpdatesContainer = findViewById(R.id.ll_updates_container);
        etComment = findViewById(R.id.et_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
        rbRating = findViewById(R.id.rb_rating);
        llCommentInput = findViewById(R.id.ll_comment_input);

        llRoleActions = findViewById(R.id.ll_role_actions);
        btnAccept = findViewById(R.id.btn_accept);
        llResolveBlock = findViewById(R.id.ll_resolve_block);
        etResolveNote = findViewById(R.id.et_resolve_note);
        btnResolve = findViewById(R.id.btn_resolve);
        llAssignBlock = findViewById(R.id.ll_assign_block);
        spAssignTechnician = findViewById(R.id.sp_assign_technician);
        btnAssign = findViewById(R.id.btn_assign);

        btnAccept.setOnClickListener(v -> acceptTicket());
        btnResolve.setOnClickListener(v -> resolveTicket());
        btnAssign.setOnClickListener(v -> assignTicket());

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ticketId = getIntent().getStringExtra("ticket_id");
        if (ticketId != null) {
            // Cache details for instant loading if available
            Ticket cached = repository.getCachedTicketDetail(ticketId);
            if (cached != null) {
                updateUI(cached);
            }
            fetchTicketDetails(ticketId);
        } else {
            Toast.makeText(this, "Error: Ticket ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSendComment.setOnClickListener(v -> submitReview());
    }

    private void fetchTicketDetails(String id) {
        apiService.getTicketDetails(id).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Ticket ticket = response.body().getData();
                    // Update cache with the fresh details
                    repository.cacheTicketDetail(ticket);
                    updateUI(ticket);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                Log.e("TicketDetails", "Error: " + t.getMessage());
            }
        });
    }

    private void updateUI(Ticket ticket) {
        if (ticket == null) return;
        currentTicket = ticket;

        tvId.setText(ticket.getId());
        tvSubject.setText(ticket.getSubject());
        tvDescription.setText(ticket.getDescription());
        tvDate.setText(ticket.getTimeAgo());

        // Display Assigned Technician
        if (ticket.getAssignedTechnician() != null && ticket.getAssignedTechnician().getName() != null) {
            tvTechnician.setText(ticket.getAssignedTechnician().getName());
        } else {
            tvTechnician.setText(R.string.not_yet_assigned);
        }

        String status = ticket.getStatus();
        tvStatus.setText(status);
        if (status != null) {
            if (status.equalsIgnoreCase("OPEN")) {
                tvStatus.setBackgroundResource(R.drawable.status_open_bg);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_open_text));
            } else if (status.equalsIgnoreCase("IN_PROGRESS")) {
                tvStatus.setBackgroundResource(R.drawable.status_progress_bg);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_progress_text));
            } else if (status.equalsIgnoreCase("RESOLVED")) {
                tvStatus.setBackgroundResource(R.drawable.status_resolved_bg);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_resolved_text));
            } else {
                tvStatus.setBackgroundResource(R.drawable.status_closed_bg);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.white));
            }
        }

        String priority = ticket.getPriority();
        tvPriority.setText(priority);
        if (priority != null) {
            if (priority.equalsIgnoreCase("HIGH")) {
                tvPriority.setBackgroundResource(R.drawable.priority_high_bg);
                tvPriority.setTextColor(ContextCompat.getColor(this, R.color.priority_high_text));
            } else if (priority.equalsIgnoreCase("MEDIUM")) {
                tvPriority.setBackgroundResource(R.drawable.priority_medium_bg);
                tvPriority.setTextColor(ContextCompat.getColor(this, R.color.priority_medium_text));
            } else if (priority.equalsIgnoreCase("LOW")) {
                tvPriority.setBackgroundResource(R.drawable.priority_low_bg);
                tvPriority.setTextColor(ContextCompat.getColor(this, R.color.priority_low_text));
            }
        }

        // Handle Status History / Updates
        llUpdatesContainer.removeAllViews();
        List<Ticket.StatusHistory> history = ticket.getStatusHistory();
        if (history != null && !history.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (Ticket.StatusHistory item : history) {
                View updateView = inflater.inflate(R.layout.item_update, llUpdatesContainer, false);
                
                TextView tvAdminName = updateView.findViewById(R.id.tv_admin_name);
                TextView tvUpdateTime = updateView.findViewById(R.id.tv_update_time);
                TextView tvUpdateNote = updateView.findViewById(R.id.tv_update_note);

                String from = item.getOldStatus() != null ? item.getOldStatus().replace('_', ' ') : "—";
                String to = item.getNewStatus() != null ? item.getNewStatus().replace('_', ' ') : "—";
                tvAdminName.setText(from + " → " + to);
                tvUpdateTime.setText(item.getChangedAt() != null ? item.getChangedAt() : "");
                tvUpdateNote.setText(item.getNote() != null ? item.getNote() : "No note provided.");
                
                llUpdatesContainer.addView(updateView);
            }
        }

        applyRoleActions(ticket);
    }

    /**
     * Shows exactly the actions that make sense for this role + this ticket's
     * current state. The review box is END_USER-only — the backend's
     * POST /tickets/{id}/review route is END_USER-only too (see routes.php),
     * so showing it to a technician/admin would just be a guaranteed 403.
     */
    private void applyRoleActions(Ticket ticket) {
        String role = tokenManager.getUserType();
        boolean isEndUser = "END_USER".equals(role) || role == null;
        llCommentInput.setVisibility(isEndUser ? View.VISIBLE : View.GONE);

        btnAccept.setVisibility(View.GONE);
        llResolveBlock.setVisibility(View.GONE);
        llAssignBlock.setVisibility(View.GONE);

        boolean unassigned = ticket.getAssignedTechnician() == null;

        if ("TECHNICIAN".equals(role)) {
            boolean assignedToMe = !unassigned && ticket.getAssignedTechnician().getId() != null
                    && ticket.getAssignedTechnician().getId().equals(tokenManager.getUserId());
            boolean canResolve = assignedToMe && "IN_PROGRESS".equalsIgnoreCase(ticket.getStatus());

            btnAccept.setVisibility(unassigned ? View.VISIBLE : View.GONE);
            llResolveBlock.setVisibility(canResolve ? View.VISIBLE : View.GONE);
            llRoleActions.setVisibility(unassigned || canResolve ? View.VISIBLE : View.GONE);
        } else if ("ADMIN".equals(role)) {
            llAssignBlock.setVisibility(unassigned ? View.VISIBLE : View.GONE);
            llRoleActions.setVisibility(unassigned ? View.VISIBLE : View.GONE);
            if (unassigned) loadTechniciansForAssign();
        } else {
            llRoleActions.setVisibility(View.GONE);
        }
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

            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                // Assign block just won't have anything to pick — acceptable, no crash.
            }
        });
    }

    private void acceptTicket() {
        btnAccept.setEnabled(false);
        apiService.acceptTicket(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                btnAccept.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(TicketDetailsActivity.this, R.string.ticket_updated, Toast.LENGTH_SHORT).show();
                    fetchTicketDetails(ticketId);
                } else {
                    Toast.makeText(TicketDetailsActivity.this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
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
                btnResolve.setEnabled(true);
                if (response.isSuccessful()) {
                    etResolveNote.setText("");
                    Toast.makeText(TicketDetailsActivity.this, R.string.ticket_updated, Toast.LENGTH_SHORT).show();
                    fetchTicketDetails(ticketId);
                } else {
                    Toast.makeText(TicketDetailsActivity.this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                btnResolve.setEnabled(true);
                Toast.makeText(TicketDetailsActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void assignTicket() {
        int pos = spAssignTechnician.getSelectedItemPosition();
        if (pos < 0 || pos >= assignableTechnicians.size()) {
            Toast.makeText(this, R.string.error_fill_required, Toast.LENGTH_SHORT).show();
            return;
        }
        String technicianId = assignableTechnicians.get(pos).getId();

        btnAssign.setEnabled(false);
        Map<String, String> body = new HashMap<>();
        body.put("technicianId", technicianId);

        apiService.assignTicket(ticketId, body).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                btnAssign.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(TicketDetailsActivity.this, R.string.ticket_updated, Toast.LENGTH_SHORT).show();
                    fetchTicketDetails(ticketId);
                } else {
                    Toast.makeText(TicketDetailsActivity.this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                btnAssign.setEnabled(true);
                Toast.makeText(TicketDetailsActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReview() {
        String comment = etComment.getText().toString();
        if (comment.isEmpty()) return;

        int rating = Math.round(rbRating.getRating());
        if (rating < 1) rating = 1; // RatingBar can report 0 if never touched; reviews need 1-5

        Map<String, Object> body = new HashMap<>();
        body.put("rating", rating);
        body.put("comment", comment);

        apiService.submitReview(ticketId, body).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    etComment.setText("");
                    fetchTicketDetails(ticketId); // Refresh details
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(TicketDetailsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
