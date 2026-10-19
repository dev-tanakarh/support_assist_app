package com.example.supportassist;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Ticket {
    @SerializedName(value = "id", alternate = {"ticket_id", "ID"})
    private String id;
    
    @SerializedName("subject")
    private String subject;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("priority")
    private String priority;
    
    @SerializedName(value = "department", alternate = {"Department", "department_name", "dept"})
    private String department;

    @SerializedName(value = "device_type", alternate = {"deviceType", "device", "DeviceType", "device_description"})
    private String deviceType;

    @SerializedName(value = "room", alternate = {"Room", "room_number", "room_id"})
    private String room;

    @SerializedName(value = "attachment_url", alternate = {"attachmentUrl", "attachment"})
    private String attachmentUrl;

    @SerializedName(value = "attachment_thumbnail_url", alternate = {"attachmentThumbnailUrl", "attachmentThumbnail"})
    private String attachmentThumbnailUrl;
    
    @SerializedName(value = "category", alternate = {"Category", "category_object"})
    private Category category;

    @SerializedName(value = "reporter", alternate = {"user", "creator", "reported_by", "Reporter"})
    private User reporter;
    
    @SerializedName(value = "time_ago", alternate = {"timeAgo"})
    private String timeAgo;
    
    @SerializedName(value = "created_at", alternate = {"createdAt"})
    private String createdAt;

    @SerializedName(value = "updated_at", alternate = {"updatedAt"})
    private String updatedAt;

    @SerializedName(value = "due_at", alternate = {"dueAt"})
    private String dueAt;

    @SerializedName(value = "resolved_at", alternate = {"resolvedAt"})
    private String resolvedAt;

    @SerializedName(value = "is_overdue", alternate = {"isOverdue"})
    private boolean isOverdue;

    @SerializedName(value = "status_history", alternate = {"statusHistory", "history"})
    private List<StatusHistory> statusHistory;

    @SerializedName(value = "assigned_technician", alternate = {"assignedTechnician", "technician", "AssignedTechnician"})
    private User assignedTechnician;

    public Ticket() { }

    /** Used by TicketRepository for robust local caching. */
    public Ticket(String id, String subject, String description, String status, String priority,
                   String timeAgo, String categoryName, String assignedTechnicianName, String attachmentUrl,
                   String department, String room, String deviceType) {
        this.id = id;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.timeAgo = timeAgo;
        this.category = categoryName != null ? new Category(categoryName) : null;
        this.assignedTechnician = assignedTechnicianName != null ? new User(assignedTechnicianName) : null;
        this.attachmentUrl = attachmentUrl;
        this.department = department;
        this.room = room;
        this.deviceType = deviceType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    
    public String getAttachmentThumbnailUrl() { return attachmentThumbnailUrl; }
    public void setAttachmentThumbnailUrl(String attachmentThumbnailUrl) { this.attachmentThumbnailUrl = attachmentThumbnailUrl; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }
    
    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    public String getDueAt() { return dueAt; }
    public void setDueAt(String dueAt) { this.dueAt = dueAt; }
    
    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public boolean isOverdue() { return isOverdue; }
    public void setOverdue(boolean overdue) { isOverdue = overdue; }
    
    public List<StatusHistory> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<StatusHistory> statusHistory) { this.statusHistory = statusHistory; }
    
    public User getAssignedTechnician() { return assignedTechnician; }
    public void setAssignedTechnician(User assignedTechnician) { this.assignedTechnician = assignedTechnician; }
    
    public boolean isUnassigned() {
        return assignedTechnician == null || assignedTechnician.getId() == null || assignedTechnician.getId().trim().isEmpty();
    }

    public String getTime() { return timeAgo; }

    public static class StatusHistory {
        @SerializedName(value = "old_status", alternate = {"oldStatus"})
        private String oldStatus;
        @SerializedName(value = "new_status", alternate = {"newStatus"})
        private String newStatus;
        @SerializedName("note")
        private String note;
        @SerializedName(value = "changed_by", alternate = {"changedBy"})
        private String changedBy;
        @SerializedName(value = "changed_at", alternate = {"changedAt"})
        private String changedAt;

        public String getOldStatus() { return oldStatus; }
        public String getNewStatus() { return newStatus; }
        public String getNote() { return note; }
        public String getChangedBy() { return changedBy; }
        public String getChangedAt() { return changedAt; }
    }
}
