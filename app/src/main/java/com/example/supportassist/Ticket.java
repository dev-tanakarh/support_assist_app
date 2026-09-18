package com.example.supportassist;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Ticket {
    private String id;
    private String subject;
    private String description;
    private String status;
    private String priority;
    private String department;

    @SerializedName("deviceType")
    private String deviceType;

    private String room;

    @SerializedName("attachmentUrl")
    private String attachmentUrl;
    
    @SerializedName("category")
    private Category category;

    @SerializedName("reporter")
    private User reporter;
    
    @SerializedName("timeAgo")
    private String timeAgo;
    
    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("dueAt")
    private String dueAt;

    @SerializedName("resolvedAt")
    private String resolvedAt;

    @SerializedName("isOverdue")
    private boolean isOverdue;

    @SerializedName("statusHistory")
    private List<StatusHistory> statusHistory;

    @SerializedName("assignedTechnician")
    private User assignedTechnician;

    /** Gson uses this via reflection for normal API responses — don't remove. */
    public Ticket() { }

    /**
     * Rebuilds a Ticket from what Room actually stored (see TicketEntity — it only
     * keeps a handful of fields for the recent-tickets offline view, not the full
     * shape the API returns). Used by TicketRepository when there's no network.
     */
    public Ticket(String id, String subject, String description, String status, String priority,
                   String timeAgo, String categoryName, String assignedTechnicianName) {
        this.id = id;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.timeAgo = timeAgo;
        this.category = categoryName != null ? new Category(categoryName) : null;
        this.assignedTechnician = assignedTechnicianName != null ? new User(assignedTechnicianName) : null;
    }

    public String getId() { return id; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public String getDepartment() { return department; }
    public String getDeviceType() { return deviceType; }
    public String getRoom() { return room; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public Category getCategory() { return category; }
    public User getReporter() { return reporter; }
    public String getTimeAgo() { return timeAgo; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getDueAt() { return dueAt; }
    public String getResolvedAt() { return resolvedAt; }
    public boolean isOverdue() { return isOverdue; }
    public List<StatusHistory> getStatusHistory() { return statusHistory; }
    public User getAssignedTechnician() { return assignedTechnician; }
    
    public String getTime() { return timeAgo; }

    public static class StatusHistory {
        @SerializedName("oldStatus")
        private String oldStatus;
        @SerializedName("newStatus")
        private String newStatus;
        private String note;
        @SerializedName("changedBy")
        private String changedBy;
        @SerializedName("changedAt")
        private String changedAt;

        public String getOldStatus() { return oldStatus; }
        public String getNewStatus() { return newStatus; }
        public String getNote() { return note; }
        public String getChangedBy() { return changedBy; }
        public String getChangedAt() { return changedAt; }
    }
}
