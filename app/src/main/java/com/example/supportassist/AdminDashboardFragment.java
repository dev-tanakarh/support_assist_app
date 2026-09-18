package com.example.supportassist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class AdminDashboardFragment extends Fragment {

    @Inject ApiService apiService;

    private ProgressBar progressBar;
    private TextView tvTotal, tvOverdue, tvAvgResolution;
    private LinearLayout llByStatus, llByPriority, llTopTechnicians;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        progressBar = view.findViewById(R.id.loading_indicator);
        tvTotal = view.findViewById(R.id.tv_stat_total);
        tvOverdue = view.findViewById(R.id.tv_stat_overdue);
        tvAvgResolution = view.findViewById(R.id.tv_stat_avg_resolution);
        llByStatus = view.findViewById(R.id.ll_by_status);
        llByPriority = view.findViewById(R.id.ll_by_priority);
        llTopTechnicians = view.findViewById(R.id.ll_top_technicians);

        loadDashboard();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
    }

    private void loadDashboard() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getDashboard().enqueue(new Callback<ApiResponse<DashboardStats>>() {
            @Override
            public void onResponse(Call<ApiResponse<DashboardStats>> call, Response<ApiResponse<DashboardStats>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.body() == null || !response.body().isSuccess()) return;
                render(response.body().getData());
            }

            @Override
            public void onFailure(Call<ApiResponse<DashboardStats>> call, Throwable t) {
                if (isAdded()) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void render(DashboardStats stats) {
        int total = 0;
        if (stats.getByStatus() != null) {
            for (DashboardStats.CountByKey row : stats.getByStatus()) total += row.getCount();
        }
        tvTotal.setText(String.valueOf(total));
        tvOverdue.setText(String.valueOf(stats.getOverdueCount()));
        tvAvgResolution.setText(formatDuration(stats.getAvgResolutionSeconds()));

        renderRows(llByStatus, stats.getByStatus(), row -> row.getStatus() != null ? row.getStatus().replace('_', ' ') : "—", DashboardStats.CountByKey::getCount);
        renderRows(llByPriority, stats.getByPriority(), DashboardStats.CountByKey::getPriority, DashboardStats.CountByKey::getCount);
        renderRows(llTopTechnicians, stats.getTopTechnicians(), DashboardStats.TopTechnician::getName, DashboardStats.TopTechnician::getResolvedCount);
    }

    /** One tiny generic helper instead of three near-identical loops — each list is just "label" + "count" rendered as a row. */
    private <T> void renderRows(LinearLayout container, List<T> items, java.util.function.Function<T, String> label, java.util.function.ToIntFunction<T> count) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText(R.string.admin_no_data);
            empty.setTextColor(getResources().getColor(R.color.text_grey, null));
            container.addView(empty);
            return;
        }
        for (T item : items) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView tvLabel = new TextView(getContext());
            tvLabel.setText(label.apply(item));
            tvLabel.setTextColor(getResources().getColor(R.color.text_main, null));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            row.addView(tvLabel, labelParams);

            TextView tvValue = new TextView(getContext());
            tvValue.setText(String.valueOf(count.applyAsInt(item)));
            tvValue.setTextColor(getResources().getColor(R.color.stat_value, null));
            tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(tvValue);

            container.addView(row);
        }
    }

    private static String formatDuration(Double seconds) {
        if (seconds == null) return "—";
        long hours = Math.round(seconds / 3600.0);
        if (hours < 1) return "<1h";
        if (hours < 48) return hours + "h";
        return String.format(Locale.getDefault(), "%.1fd", hours / 24.0);
    }
}
