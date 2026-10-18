package com.example.supportassist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ProgressBar;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
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
    @Inject TokenManager tokenManager;

    private RecyclerView rvRecentTickets;
    private TicketAdapter adapter;
    private List<Ticket> recentTicketsList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvGreeting;
    private ImageView ivProfileImage;
    private TextView tvStatOpen, tvStatInProgress, tvStatResolved;
    private TicketViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        viewModel = new ViewModelProvider(this).get(TicketViewModel.class);

        tvGreeting = view.findViewById(R.id.tv_greeting);
        ivProfileImage = view.findViewById(R.id.iv_profile_image);
        rvRecentTickets = view.findViewById(R.id.rv_recent_tickets);
        progressBar = view.findViewById(R.id.loading_indicator);
        tvStatOpen = view.findViewById(R.id.tv_stat_open_value);
        tvStatInProgress = view.findViewById(R.id.tv_stat_progress_value);
        tvStatResolved = view.findViewById(R.id.tv_stat_resolved_value);

        rvRecentTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TicketAdapter(recentTicketsList);
        rvRecentTickets.setAdapter(adapter);

        String name = tokenManager.getUserName();
        if (name == null || name.isEmpty()) name = "User";
        tvGreeting.setText(getString(R.string.hi_user, name));

        loadProfileImage();
        observeViewModel();
        loadStats();

        view.findViewById(R.id.tv_see_all).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTickets();
            }
        });

        return view;
    }

    private void loadProfileImage() {
        User user = DataRepository.getInstance(requireContext()).getCachedProfile();
        if (user != null) {
            String imageUrl = ApiClient.getFullImageUrl(user.getProfileImageUrl());
            if (imageUrl != null) {
                String token = tokenManager.getAccessToken();
                GlideUrl glideUrl = new GlideUrl(imageUrl, new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build());

                Glide.with(this)
                        .load(glideUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(ivProfileImage);
            }
        }
    }

    private void observeViewModel() {
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
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

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
            }
        });
    }
}
