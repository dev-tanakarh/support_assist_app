package com.example.supportassist;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Ticket {
    private String id;
    private String subject;
    private String description;
    private String status;
    private String priority;
    
    @SerializedName("category")
    private Category category;
    
    @SerializedName("timeAgo")
    private String timeAgo;
    
    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("statusHistory")
    private List<StatusHistory> statusHistory;

    @SerializedName("assignedTechnician")
    private User assignedTechnician;

    public String getId() { return id; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public Category getCategory() { return category; }
    public String getTimeAgo() { return timeAgo; }
    public String getCreatedAt() { return createdAt; }
    public List<StatusHistory> getStatusHistory() { return statusHistory; }
    public User getAssignedTechnician() { return assignedTechnician; }
    
    public String getTime() { return timeAgo; }

    public static class StatusHistory {
        private String status;
        private String note;
        private String createdAt;

        public String getStatus() { return status; }
        public String getNote() { return note; }
        public String getCreatedAt() { return createdAt; }
    }
}
