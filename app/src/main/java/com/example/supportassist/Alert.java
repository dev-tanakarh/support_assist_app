package com.example.supportassist;

import com.google.gson.annotations.SerializedName;

public class Alert {
    private int id;
    private String title;
    private String message;
    private String type;
    @SerializedName("colorCode")
    private String colorCode;
    @SerializedName("ticketId")
    private String ticketId;
    @SerializedName("isRead")
    private boolean isRead;
    @SerializedName("timeAgo")
    private String timeAgo;
    @SerializedName("createdAt")
    private String createdAt;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public String getColorCode() { return colorCode; }
    public String getTicketId() { return ticketId; }
    public boolean isRead() { return isRead; }
    public String getTimeAgo() { return timeAgo; }
    public String getCreatedAt() { return createdAt; }

    // Compatibility for existing adapter
    public String getTime() { return timeAgo; }
    public int getIconRes() {
        // Map type to icon resource
        if ("STATUS_CHANGE".equals(type)) return R.drawable.ic_tickets;
        return R.drawable.ic_alerts;
    }
    public int getIconBgColor() {
        try {
            return android.graphics.Color.parseColor(colorCode);
        } catch (Exception e) {
            return android.graphics.Color.GRAY;
        }
    }
}