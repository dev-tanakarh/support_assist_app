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
import com.example.supportassist.viewmodel.TicketViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;
    @Inject DataRepository dataRepository;

    private RecyclerView rvRecentTickets;
    private TicketAdapter adapter;
    private List<Ticket> recentTicketsList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvGreeting;
    private ImageView ivProfileImage;
    private TextView tvStatOpen, tvStatInProgress, tvStatResolved;
    private TicketViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

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
        tvGreeting.setText(getString(R.string.hi_user, name != null ? name : "User"));

        loadProfileImage();
        observeViewModel();
        
        // Initial sync of recent tickets (lazy loading from network to cache)
        viewModel.refreshRecentTickets();

        view.findViewById(R.id.tv_see_all).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTickets();
            }
        });

        return view;
    }

    private void loadProfileImage() {
        User user = dataRepository.getCachedProfile();
        if (user != null) {
            String avatarPath = user.getAvatarThumbnailUrl();
            if (avatarPath == null || avatarPath.isEmpty()) avatarPath = user.getProfileImageUrl();
            
            if (avatarPath != null) {
                GlideUrl glideUrl = ApiClient.getAuthenticatedGlideUrl(avatarPath, tokenManager.getAccessToken());
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
        // Reactive caching: Home screen updates immediately when the database changes
        disposables.add(viewModel.getRecentTickets()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tickets -> {
                    recentTicketsList.clear();
                    if (tickets != null) {
                        for (int i = 0; i < Math.min(tickets.size(), 4); i++) {
                            recentTicketsList.add(tickets.get(i));
                        }
                        updateStatsUI(tickets);
                    }
                    adapter.notifyDataSetChanged();
                }, throwable -> {}));

        viewModel.loading.observe(getViewLifecycleOwner(), isLoading -> {
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    private void updateStatsUI(List<Ticket> allTickets) {
        int open = 0, inProgress = 0, resolved = 0;
        for (Ticket t : allTickets) {
            String status = t.getStatus();
            if (status == null) continue;
            if (status.equalsIgnoreCase("OPEN")) open++;
            else if (status.equalsIgnoreCase("IN_PROGRESS")) inProgress++;
            else if (status.equalsIgnoreCase("RESOLVED") || status.equalsIgnoreCase("CLOSED")) resolved++;
        }
        tvStatOpen.setText(String.valueOf(open));
        tvStatInProgress.setText(String.valueOf(inProgress));
        tvStatResolved.setText(String.valueOf(resolved));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.dispose();
    }
}
