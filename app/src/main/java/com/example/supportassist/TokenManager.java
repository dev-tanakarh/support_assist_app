package com.example.supportassist;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "SupportAssistPrefs";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_USER_AVATAR = "userAvatar";
    private static final String KEY_USER_AVATAR_THUMB = "userAvatarThumb";

    private SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public void saveUser(User user) {
        if (user == null) return;
        prefs.edit()
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_USER_NAME, user.getName())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_TYPE, user.getUserType())
                .putString(KEY_USER_AVATAR, user.getProfileImageUrl())
                .putString(KEY_USER_AVATAR_THUMB, user.getAvatarThumbnailUrl())
                .apply();
    }

    public User getUser() {
        String id = getUserId();
        if (id == null) return null;
        User user = new User();
        user.setId(id);
        user.setName(getUserName());
        user.setEmail(getUserEmail());
        user.setUserType(getUserType());
        user.setProfileImageUrl(prefs.getString(KEY_USER_AVATAR, null));
        user.setAvatarThumbnailUrl(prefs.getString(KEY_USER_AVATAR_THUMB, null));
        return user;
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public String getUserType() {
        return prefs.getString(KEY_USER_TYPE, null);
    }

    public boolean isTechnician() { 
        String type = getUserType();
        return type != null && type.equalsIgnoreCase("TECHNICIAN"); 
    }
    
    public boolean isAdmin() { 
        String type = getUserType();
        return type != null && type.equalsIgnoreCase("ADMIN"); 
    }
    
    public boolean isEndUser() { 
        String type = getUserType();
        return type != null && type.equalsIgnoreCase("END_USER"); 
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
