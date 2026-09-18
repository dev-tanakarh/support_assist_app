package com.example.supportassist;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Matches AlertController::poll()'s response shape on the backend. */
public class AlertPollResponse {
    @SerializedName("alerts")
    private List<Alert> alerts;

    // Always use this as the next `since` — never the device's own clock,
    // which can drift from the server's (see RUNNING.md on the backend).
    @SerializedName("serverTime")
    private String serverTime;

    public List<Alert> getAlerts() { return alerts; }
    public String getServerTime() { return serverTime; }
}
