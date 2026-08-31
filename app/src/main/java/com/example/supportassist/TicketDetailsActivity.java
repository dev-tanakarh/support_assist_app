package com.example.supportassist;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class TicketDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_details);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Get data from intent (mocking for now since we use hardcoded data)
        String id = getIntent().getStringExtra("ticket_id");
        String subject = getIntent().getStringExtra("subject");
        String status = getIntent().getStringExtra("status");
        String priority = getIntent().getStringExtra("priority");

        TextView tvId = findViewById(R.id.tv_ticket_id);
        TextView tvSubject = findViewById(R.id.tv_subject);
        TextView tvStatus = findViewById(R.id.tv_status_tag);
        TextView tvPriority = findViewById(R.id.tv_priority_tag);
        TextView tvDescription = findViewById(R.id.tv_description);
        TextView tvDate = findViewById(R.id.tv_submitted_date);

        if (id != null) tvId.setText(id);
        if (subject != null) tvSubject.setText(subject);
        if (status != null) {
            tvStatus.setText(status);
            if (status.equalsIgnoreCase("Open")) {
                tvStatus.setBackgroundResource(R.color.status_open_bg);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_open_text));
            } else if (status.equalsIgnoreCase("In Progress")) {
                tvStatus.setBackgroundResource(R.color.status_progress_bg);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_progress_text));
            } else if (status.equalsIgnoreCase("Resolved")) {
                tvStatus.setBackgroundResource(R.color.status_resolved_bg);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_resolved_text));
            }
        }
        if (priority != null) {
            tvPriority.setText(priority);
            if (priority.equalsIgnoreCase("High")) {
                tvPriority.setBackgroundResource(R.color.priority_high_bg);
                tvPriority.setTextColor(ContextCompat.getColor(this, R.color.priority_high_text));
            } else if (priority.equalsIgnoreCase("Medium")) {
                tvPriority.setBackgroundResource(R.color.priority_medium_bg);
                tvPriority.setTextColor(ContextCompat.getColor(this, R.color.priority_medium_text));
            } else if (priority.equalsIgnoreCase("Low")) {
                tvPriority.setBackgroundResource(R.color.priority_low_bg);
                tvPriority.setTextColor(ContextCompat.getColor(this, R.color.priority_low_text));
            }
        }

        // Hardcoded details
        tvDescription.setText("The WiFi in my room keeps disconnecting every few minutes. I have tried restarting my device but the issue persists. Please assist as I have an important meeting soon.");
        tvDate.setText("01 Aug 2025 • 10:15 AM");
    }
}