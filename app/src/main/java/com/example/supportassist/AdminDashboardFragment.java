package com.example.supportassist;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import java.util.Locale;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class AdminDashboardFragment extends Fragment {

    private static final String TAG = "AdminDashboard";

    @Inject ApiService apiService;
    @Inject TokenManager tokenManager;
    @Inject DataRepository dataRepository;

    private ProgressBar progressBar;
    private TextView tvGreeting;
    private ImageView ivAdminAvatar;
    private TextView tvTotal, tvOverdue, tvAvgResolution;
    private LinearLayout llByStatus, llByPriority, llTopTechnicians;
    private RecyclerView rvRecentTickets;
    private TicketAdapter recentTicketsAdapter;
    private TicketViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        viewModel = new ViewModelProvider(this).get(TicketViewModel.class);

        progressBar = view.findViewById(R.id.loading_indicator);
        tvGreeting = view.findViewById(R.id.tv_admin_greeting);
        ivAdminAvatar = view.findViewById(R.id.iv_admin_avatar);
        tvTotal = view.findViewById(R.id.tv_stat_total);
        tvOverdue = view.findViewById(R.id.tv_stat_overdue);
        tvAvgResolution = view.findViewById(R.id.tv_stat_avg_resolution);
        llByStatus = view.findViewById(R.id.ll_by_status);
        llByPriority = view.findViewById(R.id.ll_by_priority);
        llTopTechnicians = view.findViewById(R.id.ll_top_technicians);
        rvRecentTickets = view.findViewById(R.id.rv_recent_tickets);

        recentTicketsAdapter = new TicketAdapter(new ArrayList<>());
        rvRecentTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentTickets.setAdapter(recentTicketsAdapter);

        String name = tokenManager.getUserName();
        tvGreeting.setText(getString(R.string.hi_user, name != null ? name : "Admin"));

        loadProfileImage();
        observeViewModel();
        loadDashboard();
        
        // Initial sync
        viewModel.refreshRecentTickets();
        
        view.findViewById(R.id.tv_see_all_admin).setOnClickListener(v -> {
             if (getActivity() instanceof MainActivity) {
                 ((MainActivity) getActivity()).navigateToAdminTickets();
             }
        });

        return view;
    }

    private void observeViewModel() {
        disposables.add(viewModel.getRecentTickets()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tickets -> {
                    if (tickets != null) {
                        recentTicketsAdapter.updateTickets(tickets.size() > 5 ? tickets.subList(0, 5) : tickets);
                    }
                }, throwable -> Log.e(TAG, "Error observing recent tickets", throwable)));
    }

    private void loadProfileImage() {
        User user = dataRepository.getCachedProfile();
        if (user != null && ivAdminAvatar != null) {
            String avatarPath = user.getAvatarThumbnailUrl();
            if (avatarPath == null || avatarPath.isEmpty()) avatarPath = user.getProfileImageUrl();
            
            if (avatarPath != null) {
                GlideUrl glideUrl = ApiClient.getAuthenticatedGlideUrl(avatarPath, tokenManager.getAccessToken());
                Glide.with(this)
                        .load(glideUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(ivAdminAvatar);
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
        progressBar.setVisibility(View.VISIBLE);
        apiService.getDashboard().enqueue(new Callback<ApiResponse<DashboardStats>>() {
            @Override
            public void onResponse(Call<ApiResponse<DashboardStats>> call, Response<ApiResponse<DashboardStats>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    render(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<DashboardStats>> call, Throwable t) {
                if (isAdded()) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void render(DashboardStats stats) {
        int total = 0;
        if (stats.getByStatus() != null) {
            for (DashboardStats.CountByKey row : stats.getByStatus()) total += row.getCount();
        }
        tvTotal.setText(String.valueOf(total));
        tvOverdue.setText(String.valueOf(stats.getOverdueCount()));
        tvAvgResolution.setText(formatDuration(stats.getAvgResolutionSeconds()));

        renderRows(llByStatus, stats.getByStatus(), row -> row.getStatus() != null ? row.getStatus().replace('_', ' ') : "—", DashboardStats.CountByKey::getCount);
        renderRows(llByPriority, stats.getByPriority(), DashboardStats.CountByKey::getPriority, DashboardStats.CountByKey::getCount);
        renderRows(llTopTechnicians, stats.getTopTechnicians(), DashboardStats.TopTechnician::getName, DashboardStats.TopTechnician::getResolvedCount);
    }

    private <T> void renderRows(LinearLayout container, List<T> items, java.util.function.Function<T, String> label, java.util.function.ToIntFunction<T> count) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText(R.string.admin_no_data);
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_grey));
            container.addView(empty);
            return;
        }
        for (T item : items) {
            View rowView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, container, false);
            TextView text1 = rowView.findViewById(android.R.id.text1);
            TextView text2 = rowView.findViewById(android.R.id.text2);
            
            text1.setText(label.apply(item));
            text1.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
            text1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            
            text2.setText(String.valueOf(count.applyAsInt(item)));
            text2.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_blue));
            text2.setTypeface(null, android.graphics.Typeface.BOLD);

            container.addView(rowView);
        }
    }

    private static String formatDuration(Double seconds) {
        if (seconds == null) return "—";
        long hours = Math.round(seconds / 3600.0);
        if (hours < 1) return "<1h";
        if (hours < 48) return hours + "h";
        return String.format(Locale.getDefault(), "%.1fd", hours / 24.0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.dispose();
    }
}
