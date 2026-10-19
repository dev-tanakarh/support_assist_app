package com.example.supportassist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.supportassist.viewmodel.TicketViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class TechnicianDashboardFragment extends Fragment {

    private static final String TAG = "TechDashboard";

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;
    @Inject DataRepository dataRepository;

    private ProgressBar progressBar;
    private TextView tvGreeting;
    private ImageView ivProfileImage;
    private TextView tvStatUnassigned, tvStatActive, tvStatOverdue;
    private RecyclerView rvUnassigned, rvMyActive;
    private TextView tvUnassignedEmpty, tvActiveEmpty;
    private TicketAdapter unassignedAdapter, myActiveAdapter;
    private TicketViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_technician_dashboard, container, false);

        viewModel = new ViewModelProvider(this).get(TicketViewModel.class);

        progressBar = view.findViewById(R.id.loading_indicator);
        tvGreeting = view.findViewById(R.id.tv_tech_greeting);
        ivProfileImage = view.findViewById(R.id.iv_profile_image);
        tvStatUnassigned = view.findViewById(R.id.tv_stat_unassigned);
        tvStatActive = view.findViewById(R.id.tv_stat_active);
        tvStatOverdue = view.findViewById(R.id.tv_stat_resolved_month); 
        rvUnassigned = view.findViewById(R.id.rv_unassigned);
        rvMyActive = view.findViewById(R.id.rv_my_active);
        tvUnassignedEmpty = view.findViewById(R.id.tv_unassigned_empty);
        tvActiveEmpty = view.findViewById(R.id.tv_active_empty);

        unassignedAdapter = new TicketAdapter(new ArrayList<>());
        unassignedAdapter.setUnassignedPool(true);
        rvUnassigned.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUnassigned.setAdapter(unassignedAdapter);

        myActiveAdapter = new TicketAdapter(new ArrayList<>());
        rvMyActive.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMyActive.setAdapter(myActiveAdapter);

        String name = tokenManager.getUserName();
        tvGreeting.setText(getString(R.string.hi_user, name != null ? name : "Staff"));

        loadProfileImage();
        observeViewModel();
        loadDashboard();

        view.findViewById(R.id.tv_see_all_unassigned).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTickets();
            }
        });

        view.findViewById(R.id.tv_see_all_active).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToTechQueue();
            }
        });

        return view;
    }

    private void observeViewModel() {
        // Caching: Observe recent tickets from local DB for the dashboard preview
        disposables.add(viewModel.getRecentTickets()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tickets -> {
                    if (tickets != null) {
                        List<Ticket> active = new ArrayList<>();
                        int overdueCount = 0;
                        for (Ticket t : tickets) {
                            String status = t.getStatus();
                            boolean isClosed = "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status);
                            if (!isClosed) {
                                active.add(t);
                                if (t.isOverdue()) overdueCount++;
                            }
                        }
                        
                        tvStatActive.setText(String.valueOf(active.size()));
                        tvStatOverdue.setText(String.valueOf(overdueCount));
                        
                        List<Ticket> preview = active.size() > 6 ? active.subList(0, 6) : active;
                        myActiveAdapter.updateTickets(preview);
                        tvActiveEmpty.setVisibility(active.isEmpty() ? View.VISIBLE : View.GONE);
                        rvMyActive.setVisibility(active.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }, throwable -> Log.e(TAG, "Error observing tickets", throwable)));
    }

    private void loadProfileImage() {
        User user = dataRepository.getCachedProfile();
        if (user != null && ivProfileImage != null) {
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

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard();
        loadProfileImage();
    }

    private void loadDashboard() {
        if (!isAdded()) return;
        progressBar.setVisibility(View.VISIBLE);

        // Still fetch unassigned pool from API directly for real-time accuracy if needed, 
        // but we could also move this to repository sync.
        apiService.getTicketsList(null, null, true, 1, 6).enqueue(new Callback<ApiResponse<TicketsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TicketsResponse>> call, Response<ApiResponse<TicketsResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TicketsResponse body = response.body().getData();
                    List<Ticket> unassigned = body.getTickets() != null ? body.getTickets() : new ArrayList<>();
                    tvStatUnassigned.setText(String.valueOf(body.getPagination() != null ? body.getPagination().getTotal() : unassigned.size()));
                    unassignedAdapter.updateTickets(unassigned);
                    tvUnassignedEmpty.setVisibility(unassigned.isEmpty() ? View.VISIBLE : View.GONE);
                    rvUnassigned.setVisibility(unassigned.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
            @Override public void onFailure(Call<ApiResponse<TicketsResponse>> call, Throwable t) { 
                if (isAdded()) progressBar.setVisibility(View.GONE); 
            }
        });

        // Trigger background sync for "My Active" tickets
        viewModel.refreshRecentTickets();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.dispose();
    }
}
