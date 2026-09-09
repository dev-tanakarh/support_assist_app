package com.example.supportassist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvTotalTickets, tvResolved;
    private TokenManager tokenManager;
    private ApiService apiService;
    private DataRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tokenManager = new TokenManager(getContext());
        apiService = ApiClient.getApiService(getContext());
        repository = DataRepository.getInstance(getContext());

        tvName = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_email);
        tvTotalTickets = view.findViewById(R.id.tv_total_tickets);
        tvResolved = view.findViewById(R.id.tv_resolved_tickets);

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> logout());

        loadCachedData();
        fetchProfile();
        fetchStats();

        return view;
    }

    private void loadCachedData() {
        User user = repository.getCachedProfile();
        if (user != null) {
            tvName.setText(user.getName());
            tvEmail.setText(user.getEmail());
        }
        
        List<Ticket> tickets = repository.getCachedTickets();
        if (tickets != null) {
            updateStatsUI(tickets);
        }
    }

    private void fetchProfile() {
        apiService.getProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user != null) {
                        repository.cacheProfile(user);
                        tvName.setText(user.getName());
                        tvEmail.setText(user.getEmail());
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {}
        });
    }

    private void fetchStats() {
        apiService.getTickets(null, null, null, null, 1, 100).enqueue(new Callback<ApiResponse<TicketsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TicketsResponse>> call, Response<ApiResponse<TicketsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Ticket> tickets = response.body().getData().getTickets();
                    updateStatsUI(tickets);
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<TicketsResponse>> call, Throwable t) {}
        });
    }

    private void updateStatsUI(List<Ticket> tickets) {
        int total = tickets.size();
        int resolved = 0;
        for (Ticket t : tickets) {
            if ("RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus())) {
                resolved++;
            }
        }
        tvTotalTickets.setText(String.valueOf(total));
        tvResolved.setText(String.valueOf(resolved));
    }

    private void logout() {
        tokenManager.clear();
        repository.clearCache();
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}
