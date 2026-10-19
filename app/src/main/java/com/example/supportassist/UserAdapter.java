package com.example.supportassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
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

        // Handle Profile Image - Prioritize Thumbnail (Stage 8 Performance)
        TokenManager tokenManager = new TokenManager(holder.itemView.getContext());
        String avatarPath = user.getAvatarThumbnailUrl();
        if (avatarPath == null || avatarPath.isEmpty()) {
            avatarPath = user.getProfileImageUrl();
        }

        if (avatarPath != null && !avatarPath.isEmpty()) {
            GlideUrl glideUrl = ApiClient.getAuthenticatedGlideUrl(avatarPath, tokenManager.getAccessToken());
            Glide.with(holder.itemView.getContext())
                    .load(glideUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_profile);
        }

        boolean isTechnician = "TECHNICIAN".equalsIgnoreCase(user.getUserType());
        holder.ivRemove.setVisibility(isTechnician ? View.VISIBLE : View.GONE);
        holder.ivRemove.setOnClickListener(v -> {
            if (removeListener != null) removeListener.onRemove(user);
        });

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getUserType());
        holder.swActive.setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        holder.swActive.setOnCheckedChangeListener(null); 
        holder.swActive.setChecked(user.isActive());
        holder.swActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (toggleListener != null && isChecked != user.isActive()) {
                toggleListener.onToggle(user, isChecked);
            }
        });

        if (isTechnician && user.getRating() != null) {
            holder.tvRating.setVisibility(View.VISIBLE);
            holder.tvRating.setText(holder.itemView.getContext().getString(
                    R.string.technician_rating_format, user.getRating(), user.getReviewCount()));
        } else {
            holder.tvRating.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvType, tvRating;
        ImageView ivRemove, ivAvatar;
        Switch swActive;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvType = itemView.findViewById(R.id.tv_user_type);
            tvRating = itemView.findViewById(R.id.tv_user_rating);
            ivRemove = itemView.findViewById(R.id.iv_remove_technician);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar);
            swActive = itemView.findViewById(R.id.sw_active);
        }
    }
}
