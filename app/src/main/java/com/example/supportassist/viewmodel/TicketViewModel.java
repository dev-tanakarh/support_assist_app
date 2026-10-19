package com.example.supportassist.viewmodel;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.rxjava3.PagingRx;
import com.example.supportassist.ApiService;
import com.example.supportassist.Category;
import com.example.supportassist.CategoryRepository;
import com.example.supportassist.Message;
import com.example.supportassist.MessageRepository;
import com.example.supportassist.Ticket;
import com.example.supportassist.TicketRepository;
import com.example.supportassist.paging.TicketPagingSource;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.core.Flowable;

@HiltViewModel
public class TicketViewModel extends ViewModel {

    private final ApiService apiService;
    private final TicketRepository ticketRepository;
    private final MessageRepository messageRepository;
    private final CategoryRepository categoryRepository;

    private final MutableLiveData<Boolean> _loading = new MutableLiveData<>();
    public final LiveData<Boolean> loading = _loading;

    @Inject
    public TicketViewModel(ApiService apiService, 
                           TicketRepository ticketRepository, 
                           MessageRepository messageRepository,
                           CategoryRepository categoryRepository) {
        this.apiService = apiService;
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.categoryRepository = categoryRepository;
    }

    public Flowable<List<Ticket>> getRecentTickets() {
        return ticketRepository.getRecentTickets();
    }

    public void refreshRecentTickets() {
        _loading.setValue(true);
        ticketRepository.syncRecentTickets(10);
        _loading.postValue(false); 
    }

    public Flowable<Ticket> getTicketDetails(String id) {
        return ticketRepository.getTicketById(id);
    }

    public void refreshTicketDetail(String id, @Nullable Integer unassigned) {
        ticketRepository.syncTicketDetail(id, unassigned);
    }

    public Flowable<List<Message>> getTicketMessages(String ticketId) {
        return messageRepository.getMessagesForTicket(ticketId);
    }

    public void refreshMessages(String ticketId) {
        messageRepository.syncMessages(ticketId);
    }

    public Flowable<List<Category>> getCategories() {
        return categoryRepository.getCategories();
    }

    public void refreshCategories() {
        categoryRepository.syncCategories();
    }

    /**
     * Paging 3 Stream for full ticket list with integrated backend filtering.
     * Implements lazy loading and respects server-side pagination.
     */
    public Flowable<PagingData<Ticket>> getTicketsPaging(
            @Nullable String status,
            @Nullable String priority,
            @Nullable Integer categoryId,
            @Nullable String query,
            @Nullable Boolean unassigned) {
        
        Pager<Integer, Ticket> pager = new Pager<>(
                new PagingConfig(20, 5, false),
                () -> new TicketPagingSource(apiService, status, priority, categoryId, query, unassigned)
        );
        return PagingRx.getFlowable(pager);
    }
}
