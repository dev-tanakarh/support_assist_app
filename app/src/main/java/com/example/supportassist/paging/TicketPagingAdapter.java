package com.example.supportassist.paging;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supportassist.R;
import com.example.supportassist.Ticket;

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

        public TicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tv_ticket_id);
            tvSubject = itemView.findViewById(R.id.tv_ticket_subject);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvPriority = itemView.findViewById(R.id.tv_priority);
        }

        public void bind(Ticket ticket) {
            tvId.setText(ticket.getId());
            tvSubject.setText(ticket.getSubject());
            tvStatus.setText(ticket.getStatus());
            tvTime.setText(ticket.getTimeAgo());
            tvPriority.setText(ticket.getPriority());

            // High-performance theme-aware color logic
            if (ticket.getStatus() != null) {
                String status = ticket.getStatus();
                if (status.equalsIgnoreCase("Open")) {
                    tvStatus.setBackgroundResource(R.drawable.status_open_bg);
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_open_text));
                } else if (status.equalsIgnoreCase("In Progress")) {
                    tvStatus.setBackgroundResource(R.drawable.status_progress_bg);
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.status_progress_text));
                } else {
                    tvStatus.setBackgroundResource(R.drawable.status_closed_bg);
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                }
            }
        }
    }
}
