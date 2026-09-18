package com.example.supportassist;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private List<Alert> alerts;
    private OnAlertClickListener listener;

    public interface OnAlertClickListener {
        void onAlertClick(Alert alert, int position);
    }

    public AlertAdapter(List<Alert> alerts) {
        this.alerts = alerts;
    }

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
        Alert alert = alerts.get(position);
        holder.tvTitle.setText(alert.getTitle());
        holder.tvTime.setText(alert.getTime());
        holder.ivIcon.setImageResource(alert.getIconRes());
        
        // Use the background view for the color circle as defined in item_alert.xml
        holder.vIconBg.setBackgroundTintList(ColorStateList.valueOf(alert.getIconBgColor()));

        // Visual distinction for read/unread alerts
        if (alert.isRead()) {
            holder.tvTitle.setAlpha(0.6f);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
        } else {
            holder.tvTitle.setAlpha(1.0f);
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAlertClick(alert, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return alerts.size();
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
    }
}
