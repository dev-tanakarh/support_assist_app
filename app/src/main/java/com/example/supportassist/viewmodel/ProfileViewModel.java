package com.example.supportassist.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.supportassist.ApiResponse;
import com.example.supportassist.ApiService;
import com.example.supportassist.Ticket;
import com.example.supportassist.TicketRepository;
import com.example.supportassist.User;
import com.example.supportassist.UserRepository;
import com.example.supportassist.WebSocketManager;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final ApiService apiService;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final WebSocketManager webSocketManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<User> _user = new MutableLiveData<>();
    public final LiveData<User> user = _user;

    private final MutableLiveData<Integer> _totalTickets = new MutableLiveData<>(0);
    public final LiveData<Integer> totalTickets = _totalTickets;

    private final MutableLiveData<Integer> _resolvedTickets = new MutableLiveData<>(0);
    public final LiveData<Integer> resolvedTickets = _resolvedTickets;

    private final MutableLiveData<Boolean> _logoutSuccess = new MutableLiveData<>();
    public final LiveData<Boolean> logoutSuccess = _logoutSuccess;

    @Inject
    public ProfileViewModel(ApiService apiService, 
                            UserRepository userRepository, 
                            TicketRepository ticketRepository,
                            WebSocketManager webSocketManager) {
        this.apiService = apiService;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.webSocketManager = webSocketManager;
        
        // Immediate sync-init to prevent blank screens
        User initialUser = userRepository.getCurrentUserSync();
        if (initialUser != null) {
            _user.setValue(initialUser);
        }
        
        observeProfile();
        observeStats();
        
        refreshProfile();
        fetchStats();
    }

    private void observeProfile() {
        disposables.add(userRepository.getProfile()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(u -> {
                    if (u != null) {
                        _user.setValue(u);
                    }
                }, throwable -> {}));
    }

    private void observeStats() {
        disposables.add(ticketRepository.getRecentTickets()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateStatsUI, throwable -> {}));
    }

    public void refreshProfile() {
        userRepository.syncProfile();
    }

    public void fetchStats() {
        ticketRepository.syncRecentTickets(50);
    }

    private void updateStatsUI(List<Ticket> tickets) {
        int total = tickets.size();
        int resolved = 0;
        for (Ticket t : tickets) {
            String status = t.getStatus();
            if (status != null && (status.equalsIgnoreCase("RESOLVED") || status.equalsIgnoreCase("CLOSED"))) {
                resolved++;
            }
        }
        _totalTickets.setValue(total);
        _resolvedTickets.setValue(resolved);
    }

    public void logout() {
        webSocketManager.disconnect();
        apiService.logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                _logoutSuccess.postValue(true);
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                _logoutSuccess.postValue(true);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
