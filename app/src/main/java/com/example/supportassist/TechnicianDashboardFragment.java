package com.example.supportassist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class TechnicianDashboardFragment extends Fragment {

    @Inject ApiService apiService;

    private ProgressBar progressBar;
    private TextView tvStatUnassigned, tvStatActive, tvStatResolvedMonth;
    private RecyclerView rvUnassigned, rvMyActive;
    private TextView tvUnassignedEmpty, tvActiveEmpty;
    private TicketAdapter unassignedAdapter, myActiveAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_technician_dashboard, container, false);

        progressBar = view.findViewById(R.id.loading_indicator);
        tvStatUnassigned = view.findViewById(R.id.tv_stat_unassigned);
        tvStatActive = view.findViewById(R.id.tv_stat_active);
        tvStatResolvedMonth = view.findViewById(R.id.tv_stat_resolved_month);
        rvUnassigned = view.findViewById(R.id.rv_unassigned);
        rvMyActive = view.findViewById(R.id.rv_my_active);
        tvUnassignedEmpty = view.findViewById(R.id.tv_unassigned_empty);
        tvActiveEmpty = view.findViewById(R.id.tv_active_empty);

        unassignedAdapter = new TicketAdapter(new ArrayList<>());
        rvUnassigned.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUnassigned.setAdapter(unassignedAdapter);

        myActiveAdapter = new TicketAdapter(new ArrayList<>());
        rvMyActive.setLayoutManager(new LinearLayoutManager(getContext()));
        rvMyActive.setAdapter(myActiveAdapter);

        loadDashboard();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboard(); // pick up anything accepted/resolved elsewhere since the last time this tab was visible
    }

    private void loadDashboard() {
        progressBar.setVisibility(View.VISIBLE);

        // Unassigned pool — first 6, for the preview list.
        apiService.getTicketsList(null, null, 1, 1, 6).enqueue(new Callback<ApiResponse<TicketsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TicketsResponse>> call, Response<ApiResponse<TicketsResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.body() == null || !response.body().isSuccess()) return;
                TicketsResponse body = response.body().getData();
                List<Ticket> unassigned = body.getTickets() != null ? body.getTickets() : new ArrayList<>();
                tvStatUnassigned.setText(String.valueOf(body.getPagination() != null ? body.getPagination().getTotal() : unassigned.size()));
                unassignedAdapter.updateTickets(unassigned);
                tvUnassignedEmpty.setVisibility(unassigned.isEmpty() ? View.VISIBLE : View.GONE);
                rvUnassigned.setVisibility(unassigned.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<ApiResponse<TicketsResponse>> call, Throwable t) {
                if (isAdded()) progressBar.setVisibility(View.GONE);
            }
        });

        // My tickets — backend auto-scopes to "assigned to me" for a technician
        // when `unassigned` isn't set (see routes.php's note on GET /tickets).
        apiService.getTicketsList(null, null, null, 1, 100).enqueue(new Callback<ApiResponse<TicketsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TicketsResponse>> call, Response<ApiResponse<TicketsResponse>> response) {
                if (!isAdded() || response.body() == null || !response.body().isSuccess()) return;
                List<Ticket> mine = response.body().getData().getTickets();
                if (mine == null) mine = new ArrayList<>();

                List<Ticket> active = new ArrayList<>();
                int resolvedThisMonth = 0;
                String currentYearMonth = currentYearMonth();
                for (Ticket t : mine) {
                    String status = t.getStatus();
                    boolean isDone = "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status);
                    if (!isDone) {
                        active.add(t);
                    } else if ("RESOLVED".equalsIgnoreCase(status) && isInMonth(t.getResolvedAt(), currentYearMonth)) {
                        resolvedThisMonth++;
                    }
                }

                tvStatActive.setText(String.valueOf(active.size()));
                tvStatResolvedMonth.setText(String.valueOf(resolvedThisMonth));

                List<Ticket> preview = active.size() > 6 ? active.subList(0, 6) : active;
                myActiveAdapter.updateTickets(preview);
                tvActiveEmpty.setVisibility(active.isEmpty() ? View.VISIBLE : View.GONE);
                rvMyActive.setVisibility(active.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<ApiResponse<TicketsResponse>> call, Throwable t) {
                // Unassigned-pool call already turned the spinner off; this one fails quietly.
            }
        });
    }

    private static String currentYearMonth() {
        Calendar cal = Calendar.getInstance();
        return String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
    }

    /** Timestamps come back as "2026-09-10 17:32:06.52...-07" — the "yyyy-MM" prefix is all we need, so avoid a brittle full-format parse. */
    private static boolean isInMonth(String timestamp, String yearMonth) {
        return timestamp != null && timestamp.length() >= 7 && timestamp.substring(0, 7).equals(yearMonth);
    }
}
