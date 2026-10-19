package com.example.supportassist.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    public String id;
    
    @Nullable
    public String name;
    
    @Nullable
    public String email;
    
    @Nullable
    public String phone;
    
    @Nullable
    public String userType;
    
    public boolean isActive;
    
    @Nullable
    public String profileImageUrl;
    
    @Nullable
    public String avatarThumbnailUrl;
    
    @Nullable
    public Double rating;
    
    public int reviewCount;

    public UserEntity(@NonNull String id, @Nullable String name, @Nullable String email, 
                      @Nullable String phone, @Nullable String userType, boolean isActive, 
                      @Nullable String profileImageUrl, @Nullable String avatarThumbnailUrl, 
                      @Nullable Double rating, int reviewCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.userType = userType;
        this.isActive = isActive;
        this.profileImageUrl = profileImageUrl;
        this.avatarThumbnailUrl = avatarThumbnailUrl;
        this.rating = rating;
        this.reviewCount = reviewCount;
    }
}
