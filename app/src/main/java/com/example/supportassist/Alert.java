package com.example.supportassist;

public class Alert {
    private String title;
    private String time;
    private int iconRes;
    private int iconBgColor;

    public Alert(String title, String time, int iconRes, int iconBgColor) {
        this.title = title;
        this.time = time;
        this.iconRes = iconRes;
        this.iconBgColor = iconBgColor;
    }

    public String getTitle() { return title; }
    public String getTime() { return time; }
    public int getIconRes() { return iconRes; }
    public int getIconBgColor() { return iconBgColor; }
}