package com.example.supportassist;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private List<Ticket> tickets;
    private boolean isUnassignedPool = false;

    public TicketAdapter(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public void setUnassignedPool(boolean unassignedPool) {
        this.isUnassignedPool = unassignedPool;
    }

    public void updateTickets(List<Ticket> newTickets) {
        this.tickets = newTickets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket, parent, false);
        return new TicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.bind(ticket, isUnassignedPool);
    }

    @Override
    public int getItemCount() {
        return tickets != null ? tickets.size() : 0;
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
            
            // These might not exist in the layout yet, we will add them in the XML step
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            overdueIndicator = itemView.findViewById(R.id.view_overdue_indicator);
        }

        public void bind(Ticket ticket, boolean isUnassignedPool) {
            tvId.setText(ticket.getId());
            tvSubject.setText(ticket.getSubject());
            tvStatus.setText(statusLabel(ticket.getStatus()));
            
            String timeStr = ticket.getTimeAgo() != null ? ticket.getTimeAgo() : "";
            tvTime.setText(itemView.getContext().getString(R.string.status_time_bullet, timeStr));
            
            tvPriority.setText(ticket.getPriority());

            // SLA / Overdue Indicators (Stage 3)
            if (tvDueDate != null) {
                if (ticket.getDueAt() != null) {
                    tvDueDate.setVisibility(View.VISIBLE);
                    tvDueDate.setText("Due: " + ticket.getDueAt());
                    if (ticket.isOverdue()) {
                        tvDueDate.setTextColor(Color.RED);
                    } else {
                        tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_grey));
                    }
                } else {
                    tvDueDate.setVisibility(View.GONE);
                }
            }

            if (overdueIndicator != null) {
                overdueIndicator.setVisibility(ticket.isOverdue() ? View.VISIBLE : View.GONE);
            }

            // Status Colors
            String status = ticket.getStatus();
            if (status != null) {
                int bg = R.drawable.status_open_bg, text = R.color.status_open_text;
                if (status.equalsIgnoreCase("IN_PROGRESS")) { bg = R.drawable.status_progress_bg; text = R.color.status_progress_text; }
                else if (status.equalsIgnoreCase("RESOLVED")) { bg = R.drawable.status_resolved_bg; text = R.color.status_resolved_text; }
                else if (status.equalsIgnoreCase("CLOSED")) { bg = R.drawable.status_closed_bg; text = R.color.white; }
                
                tvStatus.setBackgroundResource(bg);
                tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), text));
            }

            // Priority Colors
            String priority = ticket.getPriority();
            if (priority != null) {
                int bg = R.drawable.priority_low_bg, text = R.color.priority_low_text;
                if (priority.equalsIgnoreCase("High")) { bg = R.drawable.priority_high_bg; text = R.color.priority_high_text; }
                else if (priority.equalsIgnoreCase("Medium")) { bg = R.drawable.priority_medium_bg; text = R.color.priority_medium_text; }
                
                tvPriority.setBackgroundResource(bg);
                tvPriority.setTextColor(ContextCompat.getColor(itemView.getContext(), text));
            }

            // Stage 4: Secure Attachment Retrieval (Thumbnail)
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
                boolean shouldPassUnassigned = isUnassignedPool || (ticket.isUnassigned() && isTechOrAdmin);
                intent.putExtra("is_unassigned", shouldPassUnassigned);

                v.getContext().startActivity(intent);
            });
        }

        private String statusLabel(String status) {
            if (status == null) return "";
            return status.replace('_', ' ');
        }
    }
}
