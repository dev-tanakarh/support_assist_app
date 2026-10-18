package com.example.supportassist;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
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
    private ProgressBar progressBar;
    
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int PAGE_SIZE = 20;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        apiService = ApiClient.getApiService(getContext());
        progressBar = view.findViewById(R.id.loading_indicator);

        ImageButton btnBack = view.findViewById(R.id.btn_back_alerts);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        rvAlerts = view.findViewById(R.id.rv_alerts);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvAlerts.setLayoutManager(layoutManager);

        adapter = new AlertAdapter(alertsList);
        rvAlerts.setAdapter(adapter);

        adapter.setOnAlertClickListener((alert, position) -> {
            showAlertDetails(alert);
        });

        rvAlerts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        fetchAlerts();
                    }
                }
            }
        });

        fetchAlerts();

        return view;
    }

    private void showAlertDetails(Alert alert) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_alert_details, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

        TextView tvTitle = dialogView.findViewById(R.id.tv_alert_detail_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_alert_detail_message);
        TextView tvTime = dialogView.findViewById(R.id.tv_alert_detail_time);
        MaterialButton btnMarkRead = dialogView.findViewById(R.id.btn_mark_read);
        MaterialButton btnClose = dialogView.findViewById(R.id.btn_close_dialog);

        tvTitle.setText(alert.getTitle());
        tvMessage.setText(alert.getMessage());
        tvTime.setText(alert.getCreatedAt());

        if (alert.isRead()) {
            btnMarkRead.setVisibility(View.GONE);
        } else {
            btnMarkRead.setOnClickListener(v -> {
                markAlertRead(alert.getId());
                dialog.dismiss();
            });
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void fetchAlerts() {
        isLoading = true;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        apiService.getAlerts(currentPage, PAGE_SIZE).enqueue(new Callback<ApiResponse<List<Alert>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Alert>>> call, Response<ApiResponse<List<Alert>>> response) {
                isLoading = false;
                if (progressBar != null) progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Alert> newData = response.body().getData();
                    if (newData != null) {
                        if (currentPage == 1) {
                            alertsList.clear();
                        }
                        alertsList.addAll(newData);
                        adapter.notifyDataSetChanged();
                        
                        if (newData.size() < PAGE_SIZE) {
                            isLastPage = true;
                        } else {
                            currentPage++;
                        }
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load alerts", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Alert>>> call, Throwable t) {
                isLoading = false;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
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
                    currentPage = 1;
                    isLastPage = false;
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
