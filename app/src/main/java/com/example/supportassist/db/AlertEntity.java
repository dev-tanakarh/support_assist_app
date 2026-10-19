package com.example.supportassist.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alerts")
public class AlertEntity {
    @PrimaryKey
    public int id;
    public String title;
    public String message;
    public String type;
    public String colorCode;
    public String ticketId;
    public boolean isRead;
    public String timeAgo;
    public String createdAt;

    public AlertEntity(int id, String title, String message, String type, String colorCode, 
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
}
