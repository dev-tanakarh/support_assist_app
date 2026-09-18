package com.example.supportassist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ProgressBar;
import com.example.supportassist.viewmodel.TicketViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject ApiService apiService;

    private RecyclerView rvRecentTickets;
    private TicketAdapter adapter;
    private List<Ticket> recentTicketsList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvGreeting;
    private TextView tvStatOpen, tvStatInProgress, tvStatResolved;
    private TicketViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        viewModel = new ViewModelProvider(this).get(TicketViewModel.class);

        tvGreeting = view.findViewById(R.id.tv_greeting);
        rvRecentTickets = view.findViewById(R.id.rv_recent_tickets);
        progressBar = view.findViewById(R.id.loading_indicator);
        tvStatOpen = view.findViewById(R.id.tv_stat_open_value);
        tvStatInProgress = view.findViewById(R.id.tv_stat_progress_value);
        tvStatResolved = view.findViewById(R.id.tv_stat_resolved_value);

        rvRecentTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TicketAdapter(recentTicketsList);
        rvRecentTickets.setAdapter(adapter);

        observeViewModel();
        loadStats();

        view.findViewById(R.id.tv_see_all).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTickets();
            }
        });

        return view;
    }

    private void observeViewModel() {
        // Fixed: Observe the correct LiveData member
        viewModel.recentTickets.observe(getViewLifecycleOwner(), tickets -> {
            recentTicketsList.clear();
            if (tickets != null) {
                for (int i = 0; i < Math.min(tickets.size(), 3); i++) {
                    recentTicketsList.add(tickets.get(i));
                }
            }
            adapter.notifyDataSetChanged();
        });

        viewModel.loading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * Category tiles used to sit here and just deep-link into CreateTicketActivity
     * with a pre-filled category — replaced with a quick "where do my tickets
     * stand" summary instead, per redesign. Counts are computed client-side
     * from the user's own tickets (the backend has no per-status-count
     * endpoint for end users, only the admin dashboard does) — fine at this
     * scale (per_page 100 covers realistically anyone's ticket history).
     */
    private void loadStats() {
        apiService.getTicketsList(null, null, null, 1, 100).enqueue(new Callback<ApiResponse<TicketsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TicketsResponse>> call, Response<ApiResponse<TicketsResponse>> response) {
                if (!isAdded() || response.body() == null || !response.body().isSuccess()) return;
                List<Ticket> tickets = response.body().getData().getTickets();
                int open = 0, inProgress = 0, resolved = 0;
                if (tickets != null) {
                    for (Ticket t : tickets) {
                        String status = t.getStatus();
                        if (status == null) continue;
                        if (status.equalsIgnoreCase("OPEN")) open++;
                        else if (status.equalsIgnoreCase("IN_PROGRESS")) inProgress++;
                        else if (status.equalsIgnoreCase("RESOLVED") || status.equalsIgnoreCase("CLOSED")) resolved++;
                    }
                }
                tvStatOpen.setText(String.valueOf(open));
                tvStatInProgress.setText(String.valueOf(inProgress));
                tvStatResolved.setText(String.valueOf(resolved));
            }

            @Override
            public void onFailure(Call<ApiResponse<TicketsResponse>> call, Throwable t) {
                // Leave the placeholder dashes — this is a "nice to have"
                // summary, not worth an error toast if the network hiccups.
            }
        });
    }
}
