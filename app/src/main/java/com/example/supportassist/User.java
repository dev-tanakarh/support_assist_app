package com.example.supportassist;

import com.google.gson.annotations.SerializedName;

public class User {
    private String id;
    private String name;
    private String email;
    private String phone;
    @SerializedName("userType")
    private String userType;
    @SerializedName("isActive")
    private boolean isActive;
    @SerializedName("createdAt")
    private String createdAt;

    // Only present in responses from GET /admin/users — see User::toPublicArray
    // on the backend. Null/0 for every other response (login, profile, etc.),
    // and for non-technicians even within that endpoint.
    @SerializedName("rating")
    private Double rating;

    @SerializedName("reviewCount")
    private int reviewCount;

    public User() { }

    /** Used when rebuilding a Ticket from Room's cache, which only stores the technician's name. */
    public User(String name) {
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getUserType() { return userType; }
    public boolean isActive() { return isActive; }
    public String getCreatedAt() { return createdAt; }
    public Double getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
}