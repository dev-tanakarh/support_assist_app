package com.example.supportassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    public interface OnRemoveTechnicianListener {
        void onRemove(User user);
    }

    public interface OnToggleActiveListener {
        void onToggle(User user, boolean makeActive);
    }

    private List<User> users;
    private final OnRemoveTechnicianListener removeListener;
    private final OnToggleActiveListener toggleListener;

    public UserAdapter(List<User> users, OnRemoveTechnicianListener removeListener, OnToggleActiveListener toggleListener) {
        this.users = users;
        this.removeListener = removeListener;
        this.toggleListener = toggleListener;
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());
        holder.tvType.setText(user.getUserType());

        boolean isTechnician = "TECHNICIAN".equals(user.getUserType());
        holder.ivRemove.setVisibility(isTechnician ? View.VISIBLE : View.GONE);
        holder.ivRemove.setOnClickListener(v -> {
            if (removeListener != null) removeListener.onRemove(user);
        });

        boolean isAdmin = "ADMIN".equals(user.getUserType());
        holder.swActive.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        holder.swActive.setOnCheckedChangeListener(null); // avoid firing while we set the initial state below
        holder.swActive.setChecked(user.isActive());
        holder.swActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && isChecked != user.isActive()) {
                toggleListener.onToggle(user, isChecked);
            }
        });

        if (isTechnician && user.getRating() != null && user.getReviewCount() > 0) {
            holder.tvRating.setVisibility(View.VISIBLE);
            holder.tvRating.setText(holder.itemView.getContext().getString(
                    R.string.technician_rating_format, user.getRating(), user.getReviewCount()));
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvType, tvRating;
        ImageView ivRemove;
        Switch swActive;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvType = itemView.findViewById(R.id.tv_user_type);
            tvRating = itemView.findViewById(R.id.tv_user_rating);
            ivRemove = itemView.findViewById(R.id.iv_remove_technician);
            swActive = itemView.findViewById(R.id.sw_active);
        }
    }
}
