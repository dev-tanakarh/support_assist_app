package com.example.supportassist.paging;

import android.content.Intent;
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
                   oldItem.getSubject().equals(newItem.getSubject());
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

        public void bind(Ticket ticket) {
            tvId.setText(ticket.getId());
            tvSubject.setText(ticket.getSubject());
            tvStatus.setText(statusLabel(ticket.getStatus()));
            tvTime.setText(ticket.getTimeAgo());
            tvPriority.setText(ticket.getPriority());

            // Status Colors
            if (ticket.getStatus() != null) {
                String status = ticket.getStatus();
                if (status.equalsIgnoreCase("OPEN")) {
                    tvStatus.setBackgroundResource(R.drawable.status_open_bg);
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_open_text));
                } else if (status.equalsIgnoreCase("IN_PROGRESS")) {
                    tvStatus.setBackgroundResource(R.drawable.status_progress_bg);
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_progress_text));
                } else if (status.equalsIgnoreCase("RESOLVED")) {
                    tvStatus.setBackgroundResource(R.drawable.status_resolved_bg);
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_resolved_text));
                } else {
                    tvStatus.setBackgroundResource(R.drawable.status_closed_bg);
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                }
            }

            // Priority Colors
            if (ticket.getPriority() != null) {
                String priority = ticket.getPriority();
                if (priority.equalsIgnoreCase("High")) {
                    tvPriority.setBackgroundResource(R.drawable.priority_high_bg);
                    tvPriority.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.priority_high_text));
                } else if (priority.equalsIgnoreCase("Medium")) {
                    tvPriority.setBackgroundResource(R.drawable.priority_medium_bg);
                    tvPriority.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.priority_medium_text));
                } else if (priority.equalsIgnoreCase("Low")) {
                    tvPriority.setBackgroundResource(R.drawable.priority_low_bg);
                    tvPriority.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.priority_low_text));
                }
            }

            // Handle Attachment Thumbnail
            String imageUrl = ApiClient.getFullImageUrl(ticket.getAttachmentUrl());
            TokenManager tokenManager = new TokenManager(itemView.getContext());
            String token = tokenManager.getAccessToken();
            
            if (imageUrl != null && ivThumbnail != null) {
                ivThumbnail.setVisibility(View.VISIBLE);
                
                GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build());

                Glide.with(itemView.getContext())
                        .load(glideUrl)
                        .placeholder(R.color.card_bg)
                        .error(R.drawable.ic_logo)
                        .centerCrop()
                        .into(ivThumbnail);
            } else if (ivThumbnail != null) {
                ivThumbnail.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), TicketDetailsActivity.class);
                intent.putExtra("ticket_id", ticket.getId());
                
                // Auto-detect if we need to request unassigned access
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
