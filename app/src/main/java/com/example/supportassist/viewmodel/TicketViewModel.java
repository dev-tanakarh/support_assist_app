package com.example.supportassist.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.rxjava3.PagingRx;
import com.example.supportassist.ApiService;
import com.example.supportassist.Ticket;
import com.example.supportassist.TicketRepository;
import com.example.supportassist.paging.TicketPagingSource;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.core.Flowable;
import kotlinx.coroutines.CoroutineScope;

@HiltViewModel
public class TicketViewModel extends ViewModel {

    private final ApiService apiService;
    private final TicketRepository repository;

    private final MutableLiveData<List<Ticket>> _recentTickets = new MutableLiveData<>();
    public final LiveData<List<Ticket>> recentTickets = _recentTickets;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>();
    public final LiveData<Boolean> loading = _loading;

    @Inject
    public TicketViewModel(ApiService apiService, TicketRepository repository) {
        this.apiService = apiService;
        this.repository = repository;
        loadRecentTickets();
    }

    public void loadRecentTickets() {
        _loading.setValue(true);
        repository.syncTickets(new TicketRepository.SimpleCallback<List<Ticket>>() {
            @Override
            public void onSuccess(List<Ticket> data) {
                _recentTickets.postValue(data);
                _loading.postValue(false);
            }

            @Override
            public void onError(String message) {
                _loading.postValue(false);
            }
        });
    }

    // Paging 3 Stream for full ticket list
    public Flowable<PagingData<Ticket>> getTicketsPaging(String status) {
        Pager<Integer, Ticket> pager = new Pager<>(
                new PagingConfig(20, 5, false), // Page size, prefetch, placeholders
                () -> new TicketPagingSource(apiService, status)
        );
        return PagingRx.getFlowable(pager);
    }
}
