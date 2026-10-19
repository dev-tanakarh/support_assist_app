package com.example.supportassist;

import com.google.gson.annotations.SerializedName;

/**
 * Message model supporting both REST API and real-time WebSocket payloads.
 */
public class Message {
    @SerializedName("id")
    private String id;

    @SerializedName("body")
    private String body;

    @SerializedName(value = "is_internal", alternate = {"isInternal"})
    private boolean isInternal;

    @SerializedName(value = "created_at", alternate = {"createdAt"})
    private String createdAt;

    @SerializedName(value = "user", alternate = {"author"})
    private User user;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isInternal() { return isInternal; }
    public void setInternal(boolean internal) { isInternal = internal; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
