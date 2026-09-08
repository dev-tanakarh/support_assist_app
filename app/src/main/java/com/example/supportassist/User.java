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

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getUserType() { return userType; }
    public boolean isActive() { return isActive; }
    public String getCreatedAt() { return createdAt; }
}