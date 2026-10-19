package com.example.supportassist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();
    private final String currentUserId;

    public MessageAdapter(String currentUserId, TokenManager tokenManager) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        this.messages = new ArrayList<>(messages);
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        if (message == null) return;
        
        // Deduplication check
        for (Message m : messages) {
            // Priority 1: Compare by ID
            if (message.getId() != null && m.getId() != null) {
                if (message.getId().equals(m.getId())) return;
            }
            
            // Priority 2: Fallback check (Author + Body + CreatedAt)
            if (isSameMessage(message, m)) return;
        }
        
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    private boolean isSameMessage(Message m1, Message m2) {
        if (m1.getBody() == null || m2.getBody() == null) return false;
        if (!m1.getBody().equals(m2.getBody())) return false;
        
        String t1 = m1.getCreatedAt();
        String t2 = m2.getCreatedAt();
        if (t1 != null && t2 != null && !t1.equals(t2)) return false;

        User u1 = m1.getUser();
        User u2 = m2.getUser();
        if (u1 != null && u2 != null && u1.getId() != null && u2.getId() != null) {
            return u1.getId().equals(u2.getId());
        }
        
        return true;
    }

    public List<Message> getMessages() {
        return messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message, currentUserId);
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvAuthor, tvRole, tvBody, tvTime;
        View container;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAuthor = itemView.findViewById(R.id.tv_message_author);
            tvRole = itemView.findViewById(R.id.tv_message_role);
            tvBody = itemView.findViewById(R.id.tv_message_body);
            tvTime = itemView.findViewById(R.id.tv_message_time);
            container = itemView.findViewById(R.id.ll_message_container);
        }

        public void bind(Message message, String currentUserId) {
            tvBody.setText(message.getBody());
            tvTime.setText(formatDateTime(message.getCreatedAt()));

            User author = message.getUser();
            boolean isMe = author != null && author.getId() != null && author.getId().equals(currentUserId);
            
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) container.getLayoutParams();
            if (isMe) {
                params.horizontalBias = 1.0f; // Align to the right
                container.setBackgroundResource(R.drawable.bg_message_me);
            } else {
                params.horizontalBias = 0.0f; // Align to the left
                if (message.isInternal()) {
                    container.setBackgroundResource(R.drawable.bg_message_internal);
                } else {
                    container.setBackgroundResource(R.drawable.bg_message_other);
                }
            }
            container.setLayoutParams(params);

            if (author != null) {
                tvAuthor.setText(author.getName());
                
                String role = author.getUserType();
                if (role != null) {
                    tvRole.setText(role.toUpperCase());
                    tvRole.setVisibility(View.VISIBLE);
                } else {
                    tvRole.setVisibility(View.GONE);
                }
            } else {
                tvAuthor.setText(itemView.getContext().getString(R.string.system));
                tvRole.setVisibility(View.GONE);
            }
            
            // Apply colors based on theme/spec
            tvAuthor.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.chat_meta_light));
            tvRole.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.chat_meta_dim));
            tvTime.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.chat_meta_dim));
            tvBody.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.chat_text_bright));
        }

        private String formatDateTime(String timestamp) {
            if (timestamp == null || timestamp.isEmpty()) return "";
            try {
                // Common backend formats
                String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd HH:mm:ss"
                };
                
                Date date = null;
                for (String format : formats) {
                    try {
                        SimpleDateFormat parser = new SimpleDateFormat(format, Locale.US);
                        if (format.endsWith("'Z'")) {
                            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                        }
                        date = parser.parse(timestamp);
                        if (date != null) break;
                    } catch (Exception ignored) {}
                }

                if (date != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US);
                    return outputFormat.format(date);
                }
            } catch (Exception e) {
                return timestamp;
            }
            return timestamp;
        }
    }
}
