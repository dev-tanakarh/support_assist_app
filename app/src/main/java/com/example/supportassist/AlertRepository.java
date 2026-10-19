package com.example.supportassist;

import android.util.Log;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.rxjava3.PagingRx;
import com.example.supportassist.db.AlertDao;
import com.example.supportassist.db.AlertEntity;
import io.reactivex.rxjava3.core.Flowable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class AlertRepository {
    private static final String TAG = "AlertRepository";
    private final ApiService apiService;
    private final AlertDao alertDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public AlertRepository(ApiService apiService, AlertDao alertDao) {
        this.apiService = apiService;
        this.alertDao = alertDao;
    }

    public Flowable<PagingData<Alert>> getAlertsPaging() {
        return PagingRx.getFlowable(new Pager<>(
                new PagingConfig(20, 5, false),
                () -> alertDao.getAlertsPagingSource()
        ));
    }

    public void syncAlerts(int page, int pageSize) {
        apiService.getAlerts(page, pageSize).enqueue(new Callback<ApiResponse<List<Alert>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Alert>>> call, Response<ApiResponse<List<Alert>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Alert> alerts = response.body().getData();
                    if (alerts != null) {
                        saveAlertsLocally(alerts);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Alert>>> call, Throwable t) {
                Log.e(TAG, "Failed to sync alerts", t);
            }
        });
    }

    private void saveAlertsLocally(List<Alert> alerts) {
        List<AlertEntity> entities = new ArrayList<>();
        for (Alert a : alerts) {
            entities.add(new AlertEntity(
                    a.getId(), a.getTitle(), a.getMessage(), a.getType(),
                    a.getColorCode(), a.getTicketId(), a.isRead(),
                    a.getTimeAgo(), a.getCreatedAt()
            ));
        }
        dbExecutor.execute(() -> alertDao.insertAll(entities));
    }

    public void markAsRead(int alertId) {
        dbExecutor.execute(() -> alertDao.markAsRead(alertId));
        apiService.markAlertRead(String.valueOf(alertId)).enqueue(new Callback<ApiResponse<java.util.Map<String, String>>>() {
            @Override
            public void onResponse(Call<ApiResponse<java.util.Map<String, String>>> call, Response<ApiResponse<java.util.Map<String, String>>> response) {
                if (!response.isSuccessful()) {
                    // Optionally rollback local change if server fails, but for speed we keep local
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<java.util.Map<String, String>>> call, Throwable t) {}
        });
    }
}
