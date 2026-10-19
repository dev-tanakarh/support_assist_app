package com.example.supportassist.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey
    @NonNull
    public String id;
    
    @NonNull
    public String ticketId;
    
    @Nullable
    public String body;
    
    @Nullable
    public String authorName;
    
    @Nullable
    public String authorId;
    
    @Nullable
    public String authorRole;
    
    public boolean isInternal;
    
    @Nullable
    public String createdAt;

    public MessageEntity(@NonNull String id, @NonNull String ticketId, @Nullable String body, 
                         @Nullable String authorName, @Nullable String authorId, 
                         @Nullable String authorRole, boolean isInternal, @Nullable String createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.body = body;
        this.authorName = authorName;
        this.authorId = authorId;
        this.authorRole = authorRole;
        this.isInternal = isInternal;
        this.createdAt = createdAt;
    }
}
