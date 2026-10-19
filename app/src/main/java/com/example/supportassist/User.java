package com.example.supportassist;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName(value = "id", alternate = {"user_id"})
    private String id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("phone")
    private String phone;
    
    @SerializedName(value = "user_type", alternate = {"userType", "role"})
    private String userType;
    
    @SerializedName(value = "is_active", alternate = {"isActive", "active"})
    private boolean isActive;
    
    @SerializedName(value = "created_at", alternate = {"createdAt"})
    private String createdAt;
    
    @SerializedName(value = "profile_image_url", alternate = {"profileImageUrl", "avatar", "avatarUrl", "profile_picture"})
    private String profileImageUrl;

    @SerializedName(value = "avatar_thumbnail_url", alternate = {"avatarThumbnailUrl", "profile_thumbnail"})
    private String avatarThumbnailUrl;

    @SerializedName("rating")
    private Double rating;

    @SerializedName(value = "review_count", alternate = {"reviewCount"})
    private int reviewCount;

    public User() { }

    /** Used when rebuilding a Ticket from Room's cache, which only stores the technician's name. */
    public User(String name) {
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getAvatarThumbnailUrl() { return avatarThumbnailUrl; }
    public void setAvatarThumbnailUrl(String avatarThumbnailUrl) { this.avatarThumbnailUrl = avatarThumbnailUrl; }
}
