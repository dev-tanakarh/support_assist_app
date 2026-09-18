package com.example.supportassist;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DashboardStats {
    @SerializedName("byStatus")
    private List<CountByKey> byStatus;

    @SerializedName("byPriority")
    private List<CountByKey> byPriority;

    @SerializedName("avgResolutionSeconds")
    private Double avgResolutionSeconds; // nullable — null when no tickets have been resolved yet

    @SerializedName("overdueCount")
    private int overdueCount;

    @SerializedName("topTechnicians")
    private List<TopTechnician> topTechnicians;

    public List<CountByKey> getByStatus() { return byStatus; }
    public List<CountByKey> getByPriority() { return byPriority; }
    public Double getAvgResolutionSeconds() { return avgResolutionSeconds; }
    public int getOverdueCount() { return overdueCount; }
    public List<TopTechnician> getTopTechnicians() { return topTechnicians; }

    /** Backs both byStatus ({"status":...,"count":...}) and byPriority ({"priority":...,"count":...}) — Gson just leaves whichever key doesn't apply null. */
    public static class CountByKey {
        private String status;
        private String priority;
        private int count;

        public String getStatus() { return status; }
        public String getPriority() { return priority; }
        public int getCount() { return count; }
    }

    public static class TopTechnician {
        private String name;
        @SerializedName("resolved_count")
        private int resolvedCount;

        public String getName() { return name; }
        public int getResolvedCount() { return resolvedCount; }
    }
}
