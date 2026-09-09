package com.example.supportassist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
    private ApiService apiService;
    private DataRepository repository;
    private String ticketId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_details);

        apiService = ApiClient.getApiService(this);
        repository = DataRepository.getInstance(this);

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
                
                tvAdminName.setText("System Update: " + item.getStatus());
                tvUpdateTime.setText(item.getCreatedAt());
                tvUpdateNote.setText(item.getNote() != null ? item.getNote() : "No note provided.");
                
                llUpdatesContainer.addView(updateView);
            }
        }
    }

    private void submitReview() {
        String comment = etComment.getText().toString();
        if (comment.isEmpty()) return;

        Map<String, Object> body = new HashMap<>();
        body.put("rating", 5); 
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
