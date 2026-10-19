package com.example.supportassist.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tickets")
public class TicketEntity {
    @PrimaryKey
    @NonNull
    public String id;
    
    @Nullable
    public String subject;
    
    @Nullable
    public String description;
    
    @Nullable
    public String status;
    
    @Nullable
    public String priority;
    
    @Nullable
    public String categoryName;
    
    public int categoryId;

    @Nullable
    public String department;

    @Nullable
    public String room;

    @Nullable
    public String deviceType;
    
    @Nullable
    public String timeAgo;
    
    @Nullable
    public String reporterName;
    
    @Nullable
    public String reporterId;
    
    @Nullable
    public String assignedTechnicianName;
    
    @Nullable
    public String assignedTechnicianId;

    @Nullable
    public String attachmentUrl;

    public TicketEntity(@NonNull String id, @Nullable String subject, @Nullable String description, 
                        @Nullable String status, @Nullable String priority, @Nullable String categoryName, 
                        int categoryId, @Nullable String department, @Nullable String room, 
                        @Nullable String deviceType, @Nullable String timeAgo, 
                        @Nullable String reporterName, @Nullable String reporterId,
                        @Nullable String assignedTechnicianName, @Nullable String assignedTechnicianId,
                        @Nullable String attachmentUrl) {
        this.id = id;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.categoryName = categoryName;
        this.categoryId = categoryId;
        this.department = department;
        this.room = room;
        this.deviceType = deviceType;
        this.timeAgo = timeAgo;
        this.reporterName = reporterName;
        this.reporterId = reporterId;
        this.assignedTechnicianName = assignedTechnicianName;
        this.assignedTechnicianId = assignedTechnicianId;
        this.attachmentUrl = attachmentUrl;
    }
}
