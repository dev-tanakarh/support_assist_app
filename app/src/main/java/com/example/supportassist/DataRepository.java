package com.example.supportassist;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataRepository {
    private static final String PREF_NAME = "SupportAssistCache";
    private static final String KEY_TICKETS = "cached_tickets";
    private static final String KEY_PROFILE = "cached_profile";
    private static final String KEY_CATEGORIES = "cached_categories";
    private static final String KEY_TICKET_DETAILS_PREFIX = "ticket_detail_";
    
    private static DataRepository instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final List<DataChangeListener> listeners = new ArrayList<>();
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final ApiService apiService;

    public interface DataChangeListener {
        void onDataChanged();
    }

    private DataRepository(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        apiService = ApiClient.getApiService(context);
        startPolling();
        syncCategories(); // Pre-fetch categories on init
    }

    public static synchronized DataRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DataRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void addListener(DataChangeListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(DataChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifyChange() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            for (DataChangeListener listener : new ArrayList<>(listeners)) {
                listener.onDataChanged();
            }
        } else {
            new Handler(Looper.getMainLooper()).post(this::notifyChange);
        }
    }

    private void startPolling() {
        pollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                syncDataSilently();
                pollHandler.postDelayed(this, 15000); // Silent sync every 15s
            }
        }, 15000);
    }

    public void syncDataSilently() {
        apiService.getRecentTickets(10).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ticket>>> call, Response<ApiResponse<List<Ticket>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Ticket> freshTickets = response.body().getData();
                    cacheTickets(freshTickets);
                    
                    // Automatically cache details for tickets found in the list
                    for (Ticket t : freshTickets) {
                        cacheTicketDetail(t);
                    }
                    
                    notifyChange();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Ticket>>> call, Throwable t) {}
        });
    }

    public void syncCategories() {
        apiService.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    cacheCategories(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {}
        });
    }

    public void cacheTickets(List<Ticket> tickets) {
        prefs.edit().putString(KEY_TICKETS, gson.toJson(tickets)).apply();
    }

    public List<Ticket> getCachedTickets() {
        String json = prefs.getString(KEY_TICKETS, null);
        if (json == null) return null;
        return gson.fromJson(json, new TypeToken<List<Ticket>>(){}.getType());
    }

    public void cacheTicketDetail(Ticket ticket) {
        if (ticket != null && ticket.getId() != null) {
            prefs.edit().putString(KEY_TICKET_DETAILS_PREFIX + ticket.getId(), gson.toJson(ticket)).apply();
        }
    }

    public Ticket getCachedTicketDetail(String ticketId) {
        String json = prefs.getString(KEY_TICKET_DETAILS_PREFIX + ticketId, null);
        if (json == null) return null;
        return gson.fromJson(json, Ticket.class);
    }

    public void cacheProfile(User user) {
        prefs.edit().putString(KEY_PROFILE, gson.toJson(user)).apply();
    }

    public User getCachedProfile() {
        String json = prefs.getString(KEY_PROFILE, null);
        if (json == null) return null;
        return gson.fromJson(json, User.class);
    }

    public void cacheCategories(List<Category> categories) {
        prefs.edit().putString(KEY_CATEGORIES, gson.toJson(categories)).apply();
    }

    public List<Category> getCachedCategories() {
        String json = prefs.getString(KEY_CATEGORIES, null);
        if (json == null) return null;
        return gson.fromJson(json, new TypeToken<List<Category>>(){}.getType());
    }

    public void clearCache() {
        prefs.edit().clear().apply();
    }
}
