package com.example.supportassist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView rvRecentTickets;
    private TicketAdapter adapter;
    private List<Ticket> recentTickets = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvGreeting;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        apiService = ApiClient.getApiService(getContext());
        tvGreeting = view.findViewById(R.id.tv_greeting);
        rvRecentTickets = view.findViewById(R.id.rv_recent_tickets);
        progressBar = view.findViewById(R.id.pb_home);

        rvRecentTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TicketAdapter(recentTickets);
        rvRecentTickets.setAdapter(adapter);

        fetchProfile();
        fetchRecentTickets();

        // Setup category clicks to navigate to CreateTicketActivity
        setupCategoryClicks(view);

        return view;
    }

    private void fetchProfile() {
        apiService.getProfile().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user != null && tvGreeting != null) {
                        tvGreeting.setText("Hi " + user.getName());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Log.e("HomeFragment", "Profile Error: " + t.getMessage());
            }
        });
    }

    private void fetchRecentTickets() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        apiService.getRecentTickets(3).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ticket>>> call, Response<ApiResponse<List<Ticket>>> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    recentTickets.clear();
                    recentTickets.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Ticket>>> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Log.e("HomeFragment", "Tickets Error: " + t.getMessage());
            }
        });
    }

    private void setupCategoryClicks(View view) {
        View.OnClickListener listener = v -> {
            // In a real app, you might pass the category ID to CreateTicketActivity
            startActivity(new android.content.Intent(getActivity(), CreateTicketActivity.class));
        };
        
        view.findViewById(R.id.item_network).setOnClickListener(listener);
        view.findViewById(R.id.item_software).setOnClickListener(listener);
        view.findViewById(R.id.item_hardware).setOnClickListener(listener);
        view.findViewById(R.id.item_account).setOnClickListener(listener);
        view.findViewById(R.id.item_other).setOnClickListener(listener);
        view.findViewById(R.id.item_report).setOnClickListener(listener);
        
        view.findViewById(R.id.tv_see_all).setOnClickListener(v -> {
            // Trigger navigation to TicketsFragment - this usually involves communicating with MainActivity
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTickets();
            }
        });
    }
}
