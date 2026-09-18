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
    
    @Nullable
    public String timeAgo;
    
    @Nullable
    public String assignedTechnicianName;

    public TicketEntity(@NonNull String id, @Nullable String subject, @Nullable String description, 
                        @Nullable String status, @Nullable String priority, @Nullable String categoryName, 
                        @Nullable String timeAgo, @Nullable String assignedTechnicianName) {
        this.id = id;
        this.subject = subject;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.categoryName = categoryName;
        this.timeAgo = timeAgo;
        this.assignedTechnicianName = assignedTechnicianName;
    }
}
