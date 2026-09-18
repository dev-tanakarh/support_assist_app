package com.example.supportassist;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * The app already has full day/night color resources (values-night/colors.xml)
 * that follow the OS setting automatically via Theme.Material3.DayNight. This
 * class adds an explicit in-app override on top of that, for people who want
 * to pick a mode independent of their phone's system setting.
 */
public final class ThemeManager {
    private static final String PREF_NAME = "SupportAssistPrefs";
    private static final String KEY_THEME_MODE = "themeMode";

    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";
    public static final String MODE_SYSTEM = "system";

    private ThemeManager() { }

    public static String getSavedMode(Context context) {
        return prefs(context).getString(KEY_THEME_MODE, MODE_SYSTEM);
    }

    /** Saves the choice and applies it immediately — every open Activity redraws without needing a manual restart. */
    public static void setMode(Context context, String mode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode).apply();
        apply(mode);
    }

    /** Call once at process start (Application.onCreate) so the saved choice is already in effect before any Activity inflates. */
    public static void applySavedMode(Context context) {
        apply(getSavedMode(context));
    }

    private static void apply(String mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
