package com.example.supportassist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertsFragment extends Fragment {

    private RecyclerView rvAlerts;
    private AlertAdapter adapter;
    private List<Alert> alertsList = new ArrayList<>();
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        apiService = ApiClient.getApiService(getContext());

        ImageButton btnBack = view.findViewById(R.id.btn_back_alerts);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        rvAlerts = view.findViewById(R.id.rv_alerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AlertAdapter(alertsList);
        rvAlerts.setAdapter(adapter);

        // Set click listener for marking alerts as read
        adapter.setOnAlertClickListener((alert, position) -> {
            if (!alert.isRead()) {
                markAlertRead(alert.getId());
            }
        });

        fetchAlerts();

        return view;
    }

    private void fetchAlerts() {
        apiService.getAlerts(1, 50).enqueue(new Callback<ApiResponse<List<Alert>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Alert>>> call, Response<ApiResponse<List<Alert>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    alertsList.clear();
                    alertsList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Failed to load alerts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Alert>>> call, Throwable t) {
                Log.e("AlertsFragment", "Error: " + t.getMessage());
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAlertRead(int id) {
        apiService.markAlertRead(id).enqueue(new Callback<ApiResponse<Map<String, String>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, String>>> call, Response<ApiResponse<Map<String, String>>> response) {
                if (response.isSuccessful()) {
                    // Refresh alerts to update UI
                    fetchAlerts();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, String>>> call, Throwable t) {
                Log.e("AlertsFragment", "Error marking read: " + t.getMessage());
            }
        });
    }
}