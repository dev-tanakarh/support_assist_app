package com.example.supportassist;

import android.content.Intent;
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

    /** 
     * Explicitly marks this adapter as showing the unassigned pool.
     */
    public void setUnassignedPool(boolean unassignedPool) {
        this.isUnassignedPool = unassignedPool;
    }

    /** Swap in a fresh list without recreating the adapter/RecyclerView state. */
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
        holder.tvId.setText(ticket.getId());
        holder.tvSubject.setText(ticket.getSubject());
        holder.tvStatus.setText(statusLabel(ticket.getStatus()));
        
        String timeStr = ticket.getTimeAgo() != null ? ticket.getTimeAgo() : "";
        holder.tvTime.setText(holder.itemView.getContext().getString(R.string.status_time_bullet, timeStr));
        
        holder.tvPriority.setText(ticket.getPriority());

        // Set status colors
        String status = ticket.getStatus();
        if (status != null) {
            if (status.equalsIgnoreCase("OPEN")) {
                holder.tvStatus.setBackgroundResource(R.drawable.status_open_bg);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_open_text));
            } else if (status.equalsIgnoreCase("IN_PROGRESS")) {
                holder.tvStatus.setBackgroundResource(R.drawable.status_progress_bg);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_progress_text));
            } else if (status.equalsIgnoreCase("RESOLVED")) {
                holder.tvStatus.setBackgroundResource(R.drawable.status_resolved_bg);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_resolved_text));
            } else {
                holder.tvStatus.setBackgroundResource(R.drawable.status_closed_bg);
                holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            }
        }

        // Set priority colors
        if (ticket.getPriority() != null) {
            String priority = ticket.getPriority();
            if (priority.equalsIgnoreCase("High")) {
                holder.tvPriority.setBackgroundResource(R.drawable.priority_high_bg);
                holder.tvPriority.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.priority_high_text));
            } else if (priority.equalsIgnoreCase("Medium")) {
                holder.tvPriority.setBackgroundResource(R.drawable.priority_medium_bg);
                holder.tvPriority.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.priority_medium_text));
            } else if (priority.equalsIgnoreCase("Low")) {
                holder.tvPriority.setBackgroundResource(R.drawable.priority_low_bg);
                holder.tvPriority.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.priority_low_text));
            }
        }

        // Handle Attachment Thumbnail
        String imageUrl = ApiClient.getFullImageUrl(ticket.getAttachmentUrl());
        TokenManager tokenManager = new TokenManager(holder.itemView.getContext());
        String token = tokenManager.getAccessToken();

        if (imageUrl != null) {
            holder.ivThumbnail.setVisibility(View.VISIBLE);
            
            GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build());

            Glide.with(holder.itemView.getContext())
                    .load(glideUrl)
                    .placeholder(R.color.card_bg)
                    .error(R.drawable.ic_logo) 
                    .centerCrop()
                    .into(holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TicketDetailsActivity.class);
            intent.putExtra("ticket_id", ticket.getId());
            
            // Auto-detect if we need to request unassigned access (for Technicians/Admins)
            boolean isTechOrAdmin = tokenManager.isTechnician() || tokenManager.isAdmin();
            
            // Pass true if it's explicitly from the unassigned pool or if auto-detected as unassigned
            boolean shouldPassUnassigned = isUnassignedPool || (ticket.isUnassigned() && isTechOrAdmin);
            intent.putExtra("is_unassigned", shouldPassUnassigned);

            v.getContext().startActivity(intent);
        });
    }

    private static String statusLabel(String status) {
        if (status == null) return "";
        return status.replace('_', ' ');
    }

    @Override
    public int getItemCount() {
        return tickets != null ? tickets.size() : 0;
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvSubject, tvStatus, tvTime, tvPriority;
        ImageView ivThumbnail;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tv_ticket_id);
            tvSubject = itemView.findViewById(R.id.tv_ticket_subject);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvPriority = itemView.findViewById(R.id.tv_priority);
            ivThumbnail = itemView.findViewById(R.id.iv_ticket_thumbnail);
        }
    }
}
