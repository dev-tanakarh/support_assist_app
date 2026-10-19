package com.example.supportassist.paging;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.supportassist.ApiClient;
import com.example.supportassist.R;
import com.example.supportassist.Ticket;
import com.example.supportassist.TicketDetailsActivity;
import com.example.supportassist.TokenManager;

public class TicketPagingAdapter extends PagingDataAdapter<Ticket, TicketPagingAdapter.TicketViewHolder> {

    public TicketPagingAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Ticket> DIFF_CALLBACK = new DiffUtil.ItemCallback<Ticket>() {
        @Override
        public boolean areItemsTheSame(@NonNull Ticket oldItem, @NonNull Ticket newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Ticket oldItem, @NonNull Ticket newItem) {
            return oldItem.getStatus().equals(newItem.getStatus()) && 
                   oldItem.getSubject().equals(newItem.getSubject()) &&
                   oldItem.isOverdue() == newItem.isOverdue();
        }
    };

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = getItem(position);
        if (ticket != null) {
            holder.bind(ticket);
        }
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvSubject, tvStatus, tvTime, tvPriority, tvDueDate;
        ImageView ivThumbnail;
        View overdueIndicator;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tv_ticket_id);
            tvSubject = itemView.findViewById(R.id.tv_ticket_subject);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvPriority = itemView.findViewById(R.id.tv_priority);
            ivThumbnail = itemView.findViewById(R.id.iv_ticket_thumbnail);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            overdueIndicator = itemView.findViewById(R.id.view_overdue_indicator);
        }

        public void bind(Ticket ticket) {
            tvId.setText(ticket.getId());
            tvSubject.setText(ticket.getSubject());
            tvStatus.setText(statusLabel(ticket.getStatus()));
            tvTime.setText(ticket.getTimeAgo());
            tvPriority.setText(ticket.getPriority());

            // SLA / Overdue logic (Stage 3)
            if (ticket.getDueAt() != null) {
                tvDueDate.setVisibility(View.VISIBLE);
                tvDueDate.setText(itemView.getContext().getString(R.string.placeholder_dash) + " Due: " + ticket.getDueAt());
                tvDueDate.setTextColor(ticket.isOverdue() ? Color.RED : ContextCompat.getColor(itemView.getContext(), R.color.text_grey));
            } else {
                tvDueDate.setVisibility(View.GONE);
            }
            
            if (overdueIndicator != null) {
                overdueIndicator.setVisibility(ticket.isOverdue() ? View.VISIBLE : View.GONE);
            }

            // Status Colors
            if (ticket.getStatus() != null) {
                String status = ticket.getStatus();
                int bg = R.drawable.status_open_bg, text = R.color.status_open_text;
                if (status.equalsIgnoreCase("IN_PROGRESS")) { bg = R.drawable.status_progress_bg; text = R.color.status_progress_text; }
                else if (status.equalsIgnoreCase("RESOLVED")) { bg = R.drawable.status_resolved_bg; text = R.color.status_resolved_text; }
                else if (status.equalsIgnoreCase("CLOSED")) { bg = R.drawable.status_closed_bg; text = R.color.white; }
                
                tvStatus.setBackgroundResource(bg);
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), text));
            }

            // Priority Colors
            if (ticket.getPriority() != null) {
                String priority = ticket.getPriority();
                int bg = R.drawable.priority_low_bg, text = R.color.priority_low_text;
                if (priority.equalsIgnoreCase("High")) { bg = R.drawable.priority_high_bg; text = R.color.priority_high_text; }
                else if (priority.equalsIgnoreCase("Medium")) { bg = R.drawable.priority_medium_bg; text = R.color.priority_medium_text; }
                
                tvPriority.setBackgroundResource(bg);
                tvPriority.setTextColor(ContextCompat.getColor(itemView.getContext(), text));
            }

            // Secure Thumbnail Retrieval (Stage 4)
            TokenManager tokenManager = new TokenManager(itemView.getContext());
            if (ticket.getAttachmentUrl() != null && !ticket.getAttachmentUrl().isEmpty()) {
                ivThumbnail.setVisibility(View.VISIBLE);
                String secureThumbUrl = ApiClient.getBaseUrl() + "tickets/" + ticket.getId() + "/attachment?variant=thumbnail";
                GlideUrl glideUrl = new GlideUrl(secureThumbUrl, new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + tokenManager.getAccessToken())
                        .build());

                Glide.with(itemView.getContext())
                        .load(glideUrl)
                        .placeholder(R.color.card_bg)
                        .error(R.drawable.ic_logo)
                        .centerCrop()
                        .into(ivThumbnail);
            } else {
                ivThumbnail.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), TicketDetailsActivity.class);
                intent.putExtra("ticket_id", ticket.getId());
                
                boolean isTechOrAdmin = tokenManager.isTechnician() || tokenManager.isAdmin();
                intent.putExtra("is_unassigned", ticket.isUnassigned() && isTechOrAdmin);
                v.getContext().startActivity(intent);
            });
        }

        private String statusLabel(String status) {
            if (status == null) return "";
            return status.replace('_', ' ');
        }
    }
}
