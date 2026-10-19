package com.example.supportassist.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.paging.PagingData;
import com.example.supportassist.Alert;
import com.example.supportassist.AlertRepository;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.core.Flowable;

@HiltViewModel
public class AlertViewModel extends ViewModel {

    private final AlertRepository repository;

    @Inject
    public AlertViewModel(AlertRepository repository) {
        this.repository = repository;
    }

    public Flowable<PagingData<Alert>> getAlertsPaging() {
        return repository.getAlertsPaging();
    }

    public void syncAlerts(int page, int pageSize) {
        repository.syncAlerts(page, pageSize);
    }

    public void markAsRead(int alertId) {
        repository.markAsRead(alertId);
    }
}
