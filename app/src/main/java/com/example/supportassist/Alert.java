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

    public Alert() {}

    public Alert(int id, String title, String message, String type, String colorCode, 
                 String ticketId, boolean isRead, String timeAgo, String createdAt) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.colorCode = colorCode;
        this.ticketId = ticketId;
        this.isRead = isRead;
        this.timeAgo = timeAgo;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // Compatibility for existing adapter
    public String getTime() { return timeAgo; }
    public int getIconRes() {
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
