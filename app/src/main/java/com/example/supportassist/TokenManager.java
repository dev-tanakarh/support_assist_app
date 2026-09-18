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

    /** Called right after login/signup so role-based UI never needs an extra round trip just to find out who's logged in. */
    public void saveUser(User user) {
        if (user == null) return;
        prefs.edit()
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_USER_NAME, user.getName())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_TYPE, user.getUserType())
                .apply();
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

    /** "END_USER", "TECHNICIAN", or "ADMIN" — see User::toPublicArray on the backend. Null if never saved (shouldn't happen post-login). */
    public String getUserType() {
        return prefs.getString(KEY_USER_TYPE, null);
    }

    public boolean isTechnician() { return "TECHNICIAN".equals(getUserType()); }
    public boolean isAdmin() { return "ADMIN".equals(getUserType()); }
    public boolean isEndUser() { return "END_USER".equals(getUserType()); }

    public void clear() {
        prefs.edit().clear().apply();
    }
}