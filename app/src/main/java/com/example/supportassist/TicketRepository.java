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
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class TicketRepository {
    private static final String TAG = "TicketRepository";

    private final ApiService apiService;
    private final TicketDao ticketDao;
    private final TokenManager tokenManager;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public TicketRepository(ApiService apiService, TicketDao ticketDao, TokenManager tokenManager) {
        this.apiService = apiService;
        this.ticketDao = ticketDao;
        this.tokenManager = tokenManager;
    }

    public Flowable<Ticket> getTicketById(String id) {
        return ticketDao.getTicketById(id)
                .map(this::mapToDomain)
                .subscribeOn(Schedulers.io());
    }

    public Flowable<List<Ticket>> getRecentTickets() {
        String userId = tokenManager.getUserId();
        boolean isStaff = tokenManager.isTechnician() || tokenManager.isAdmin();
        
        return ticketDao.getRecentTicketsFiltered(userId, isStaff)
                .map(entities -> {
                    List<Ticket> tickets = new ArrayList<>();
                    for (TicketEntity e : entities) {
                        tickets.add(mapToDomain(e));
                    }
                    return tickets;
                })
                .subscribeOn(Schedulers.io());
    }

    public void syncTicketDetail(String id, Integer unassigned) {
        apiService.getTicketDetails(id, unassigned).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Ticket ticket = response.body().getData();
                    if (ticket != null) {
                        saveTicketLocally(ticket);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                Log.e(TAG, "Sync ticket failed: " + t.getMessage());
            }
        });
    }

    public void syncRecentTickets(int limit) {
        apiService.getRecentTickets(limit).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ticket>>> call, Response<ApiResponse<List<Ticket>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Ticket> tickets = response.body().getData();
                    if (tickets != null) {
                        saveTicketsLocally(tickets);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Ticket>>> call, Throwable t) {
                Log.e(TAG, "Sync recent tickets failed", t);
            }
        });
    }

    private void saveTicketsLocally(List<Ticket> tickets) {
        List<TicketEntity> entities = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.getId() == null) continue;
            entities.add(mapToEntity(t));
        }
        dbExecutor.execute(() -> {
            try {
                ticketDao.insertAll(entities);
            } catch (Exception e) {
                Log.e(TAG, "Error saving tickets locally", e);
            }
        });
    }

    private void saveTicketLocally(Ticket t) {
        dbExecutor.execute(() -> {
            try {
                ticketDao.insert(mapToEntity(t));
            } catch (Exception e) {
                Log.e(TAG, "Error saving single ticket", e);
            }
        });
    }

    private Ticket mapToDomain(TicketEntity e) {
        if (e == null) return null;
        Ticket t = new Ticket(e.id, e.subject, e.description, e.status, e.priority,
                e.timeAgo, e.categoryName, e.assignedTechnicianName, e.attachmentUrl,
                e.department, e.room, e.deviceType);
        
        if (e.categoryId > 0) {
            t.setCategory(new Category(e.categoryId, e.categoryName));
        }
        
        if (e.reporterId != null) {
            User reporter = new User(e.reporterName);
            reporter.setId(e.reporterId);
            t.setReporter(reporter);
        }
        
        if (e.assignedTechnicianId != null) {
            User tech = new User(e.assignedTechnicianName);
            tech.setId(e.assignedTechnicianId);
            t.setAssignedTechnician(tech);
        }
        
        return t;
    }

    private TicketEntity mapToEntity(Ticket t) {
        String reporterName = t.getReporter() != null ? t.getReporter().getName() : null;
        String reporterId = t.getReporter() != null ? t.getReporter().getId() : null;
        String techName = t.getAssignedTechnician() != null ? t.getAssignedTechnician().getName() : null;
        String techId = t.getAssignedTechnician() != null ? t.getAssignedTechnician().getId() : null;
        int categoryId = t.getCategory() != null ? t.getCategory().getId() : 0;
        String categoryName = t.getCategory() != null ? t.getCategory().getName() : null;

        return new TicketEntity(
                t.getId(),
                t.getSubject(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                categoryName,
                categoryId,
                t.getDepartment(),
                t.getRoom(),
                t.getDeviceType(),
                t.getTimeAgo(),
                reporterName,
                reporterId,
                techName,
                techId,
                t.getAttachmentUrl()
        );
    }

    public interface SimpleCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }
}
