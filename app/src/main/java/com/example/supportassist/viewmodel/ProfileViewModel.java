package com.example.supportassist.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.supportassist.ApiResponse;
import com.example.supportassist.ApiService;
import com.example.supportassist.DataRepository;
import com.example.supportassist.Ticket;
import com.example.supportassist.TicketsResponse;
import com.example.supportassist.User;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ApiService apiService;
    private final DataRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<User> _user = new MutableLiveData<>();
    public final LiveData<User> user = _user;

    private final MutableLiveData<Integer> _totalTickets = new MutableLiveData<>(0);
    public final LiveData<Integer> totalTickets = _totalTickets;

    private final MutableLiveData<Integer> _resolvedTickets = new MutableLiveData<>(0);
    public final LiveData<Integer> resolvedTickets = _resolvedTickets;

    @Inject
    public ProfileViewModel(ApiService apiService, DataRepository repository) {
        this.apiService = apiService;
        this.repository = repository;
        
        // Load cached user immediately
        _user.setValue(repository.getCachedProfile());
        refreshProfile();
        fetchStats();
    }

    public void refreshProfile() {
        apiService.getProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body().getData();
                    repository.cacheProfile(user);
                    _user.postValue(user);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {}
        });
    }

    public void fetchStats() {
        disposables.add(
            apiService.getTickets(null, null, null, null, 1, 100)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    if (response.isSuccess()) {
                        List<Ticket> tickets = response.getData().getTickets();
                        updateStatsUI(tickets);
                    }
                }, throwable -> {})
        );
    }

    private void updateStatsUI(List<Ticket> tickets) {
        int total = tickets.size();
        int resolved = 0;
        for (Ticket t : tickets) {
            if ("RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus())) {
                resolved++;
            }
        }
        _totalTickets.setValue(total);
        _resolvedTickets.setValue(resolved);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
