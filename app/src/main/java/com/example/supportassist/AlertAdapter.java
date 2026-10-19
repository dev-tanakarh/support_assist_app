package com.example.supportassist;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

public class AlertAdapter extends PagingDataAdapter<Alert, AlertAdapter.AlertViewHolder> {

    private OnAlertClickListener listener;

    public interface OnAlertClickListener {
        void onAlertClick(Alert alert, int position);
    }

    public AlertAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Alert> DIFF_CALLBACK = new DiffUtil.ItemCallback<Alert>() {
        @Override
        public boolean areItemsTheSame(@NonNull Alert oldItem, @NonNull Alert newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Alert oldItem, @NonNull Alert newItem) {
            return oldItem.isRead() == newItem.isRead() && 
                   oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.getTimeAgo().equals(newItem.getTimeAgo());
        }
    };

    public void setOnAlertClickListener(OnAlertClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alert alert = getItem(position);
        if (alert != null) {
            holder.bind(alert, position, listener);
        }
    }

    public static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        ImageView ivIcon;
        View vIconBg;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_alert_title);
            tvTime = itemView.findViewById(R.id.tv_alert_time);
            ivIcon = itemView.findViewById(R.id.iv_alert_icon);
            vIconBg = itemView.findViewById(R.id.v_icon_bg);
        }

        public void bind(Alert alert, int position, OnAlertClickListener listener) {
            tvTitle.setText(alert.getTitle());
            tvTime.setText(alert.getTime());
            ivIcon.setImageResource(alert.getIconRes());
            
            vIconBg.setBackgroundTintList(ColorStateList.valueOf(alert.getIconBgColor()));

            if (alert.isRead()) {
                tvTitle.setAlpha(0.6f);
                tvTitle.setTypeface(null, Typeface.NORMAL);
            } else {
                tvTitle.setAlpha(1.0f);
                tvTitle.setTypeface(null, Typeface.BOLD);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlertClick(alert, position);
                }
            });
        }
    }
}
