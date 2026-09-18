package com.example.supportassist;

import android.util.Log;
import com.example.supportassist.db.TicketDao;
import com.example.supportassist.db.TicketEntity;
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
public class TicketRepository {
    private static final String TAG = "TicketRepository";

    private final ApiService apiService;
    private final TicketDao ticketDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public TicketRepository(ApiService apiService, TicketDao ticketDao) {
        this.apiService = apiService;
        this.ticketDao = ticketDao;
    }

    public void syncTickets(final SimpleCallback<List<Ticket>> callback) {
        apiService.getRecentTickets(10).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ticket>>> call, Response<ApiResponse<List<Ticket>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ticket> tickets = response.body().getData();
                    if (tickets != null) {
                        saveTicketsLocally(tickets);
                        callback.onSuccess(tickets);
                    } else {
                        loadFromCache(callback, "Empty data from server");
                    }
                } else {
                    loadFromCache(callback, "Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Ticket>>> call, Throwable t) {
                loadFromCache(callback, t.getMessage());
            }
        });
    }

    private void loadFromCache(SimpleCallback<List<Ticket>> callback, String reasonForFallback) {
        dbExecutor.execute(() -> {
            try {
                List<TicketEntity> cached = ticketDao.getAllTickets();
                if (cached == null || cached.isEmpty()) {
                    callback.onError(reasonForFallback);
                    return;
                }
                Log.d(TAG, "Serving " + cached.size() + " cached tickets (reason: " + reasonForFallback + ")");
                List<Ticket> tickets = new ArrayList<>();
                for (TicketEntity e : cached) {
                    tickets.add(new Ticket(e.id, e.subject, e.description, e.status, e.priority,
                            e.timeAgo, e.categoryName, e.assignedTechnicianName));
                }
                callback.onSuccess(tickets);
            } catch (Exception e) {
                Log.e(TAG, "Error loading from cache", e);
                callback.onError(reasonForFallback);
            }
        });
    }

    private void saveTicketsLocally(List<Ticket> tickets) {
        List<TicketEntity> entities = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.getId() == null) continue; // Skip tickets without ID
            
            entities.add(new TicketEntity(
                t.getId(), 
                t.getSubject(), 
                t.getDescription(), 
                t.getStatus(), 
                t.getPriority(), 
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getTimeAgo(),
                t.getAssignedTechnician() != null ? t.getAssignedTechnician().getName() : null
            ));
        }
        dbExecutor.execute(() -> {
            try {
                ticketDao.clearAll();
                if (!entities.isEmpty()) {
                    ticketDao.insertAll(entities);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving tickets locally", e);
            }
        });
    }

    public interface SimpleCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}
