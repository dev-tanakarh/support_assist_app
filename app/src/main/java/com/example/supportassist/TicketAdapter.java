package com.example.supportassist;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.TicketViewHolder> {

    private List<Ticket> tickets;

    public TicketAdapter(List<Ticket> tickets) {
        this.tickets = tickets;
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

        // Set status colors. Matched against the backend's actual enum values
        // (OPEN / IN_PROGRESS / RESOLVED / CLOSED) — this used to compare
        // against "In Progress" (space, title case), which never matched
        // "IN_PROGRESS" and silently mis-colored every in-progress ticket.
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

        // Priority already matched correctly (HIGH/MEDIUM/LOW have no case-vs-underscore mismatch), left as-is.
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

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), TicketDetailsActivity.class);
                intent.putExtra("ticket_id", ticket.getId());
                v.getContext().startActivity(intent);
            }
        });
    }

    private static String statusLabel(String status) {
        if (status == null) return "";
        return status.replace('_', ' ');
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    public static class TicketViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvSubject, tvStatus, tvTime, tvPriority;

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tv_ticket_id);
            tvSubject = itemView.findViewById(R.id.tv_ticket_subject);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvPriority = itemView.findViewById(R.id.tv_priority);
        }
    }
}
