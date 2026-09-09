package com.example.supportassist;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment implements DataRepository.DataChangeListener {

    private RecyclerView rvRecentTickets;
    private TicketAdapter adapter;
    private List<Ticket> recentTickets = new ArrayList<>();
    private LinearProgressIndicator progressBar;
    private TextView tvGreeting;
    private ApiService apiService;
    private DataRepository repository;
    private boolean isInitialLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        apiService = ApiClient.getApiService(getContext());
        repository = DataRepository.getInstance(getContext());
        repository.addListener(this);

        tvGreeting = view.findViewById(R.id.tv_greeting);
        rvRecentTickets = view.findViewById(R.id.rv_recent_tickets);
        progressBar = view.findViewById(R.id.pb_home);

        rvRecentTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TicketAdapter(recentTickets);
        rvRecentTickets.setAdapter(adapter);

        // Load instantly from cache
        loadCachedData();
        
        fetchProfile();
        
        // Fetch most recent 3 tickets as requested
        boolean shouldShowProgress = isInitialLoad && recentTickets.isEmpty();
        fetchRecentTickets(shouldShowProgress);

        setupCategoryClicks(view);

        return view;
    }

    private void loadCachedData() {
        User cachedUser = repository.getCachedProfile();
        if (cachedUser != null) {
            tvGreeting.setText("Hi " + cachedUser.getName());
        }

        List<Ticket> cachedTickets = repository.getCachedTickets();
        if (cachedTickets != null) {
            recentTickets.clear();
            // Show exactly 3 recent tickets
            for (int i = 0; i < Math.min(cachedTickets.size(), 3); i++) {
                recentTickets.add(cachedTickets.get(i));
            }
            adapter.notifyDataSetChanged();
            isInitialLoad = false;
        }
    }

    private void fetchProfile() {
        apiService.getProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body().getData();
                    if (user != null) {
                        repository.cacheProfile(user);
                        if (tvGreeting != null) tvGreeting.setText("Hi " + user.getName());
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {}
        });
    }

    private void fetchRecentTickets(boolean showProgress) {
        if (showProgress && progressBar != null) progressBar.setVisibility(View.VISIBLE);

        apiService.getRecentTickets(3).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ticket>>> call, Response<ApiResponse<List<Ticket>>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                isInitialLoad = false;
                
                if (response.isSuccessful() && response.body() != null) {
                    List<Ticket> tickets = response.body().getData();
                    repository.cacheTickets(tickets);
                    
                    // Cache individual ticket details for instant loading
                    for (Ticket t : tickets) {
                        repository.cacheTicketDetail(t);
                    }

                    recentTickets.clear();
                    for (int i = 0; i < Math.min(tickets.size(), 3); i++) {
                        recentTickets.add(tickets.get(i));
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Ticket>>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void setupCategoryClicks(View view) {
        View.OnClickListener listener = v -> {
            Intent intent = new Intent(getActivity(), CreateTicketActivity.class);
            int id = v.getId();
            if (id == R.id.item_network) {
                intent.putExtra("category_id", 1);
                intent.putExtra("subject_hint", "Network Issue: ");
            } else if (id == R.id.item_software) {
                intent.putExtra("category_id", 2);
                intent.putExtra("subject_hint", "Software Bug: ");
            } else if (id == R.id.item_hardware) {
                intent.putExtra("category_id", 3);
                intent.putExtra("subject_hint", "Hardware Fault: ");
            } else if (id == R.id.item_account) {
                intent.putExtra("category_id", 4);
                intent.putExtra("subject_hint", "Account Access: ");
            }
            startActivity(intent);
        };
        
        view.findViewById(R.id.item_network).setOnClickListener(listener);
        view.findViewById(R.id.item_software).setOnClickListener(listener);
        view.findViewById(R.id.item_hardware).setOnClickListener(listener);
        view.findViewById(R.id.item_account).setOnClickListener(listener);
        view.findViewById(R.id.item_other).setOnClickListener(listener);
        view.findViewById(R.id.item_report).setOnClickListener(listener);
        
        view.findViewById(R.id.tv_see_all).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTickets();
            }
        });
    }

    @Override
    public void onDataChanged() {
        loadCachedData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        repository.removeListener(this);
    }
}
