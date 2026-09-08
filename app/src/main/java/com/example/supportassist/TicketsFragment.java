package com.example.supportassist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketsFragment extends Fragment {

    private RecyclerView rvTickets;
    private TicketAdapter adapter;
    private List<Ticket> allTickets = new ArrayList<>();
    private TextView tvFilterAll, tvFilterOpen, tvFilterProgress, tvFilterClosed;
    private String currentStatus = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tickets, container, false);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        tvFilterAll = view.findViewById(R.id.tv_filter_all);
        tvFilterOpen = view.findViewById(R.id.tv_filter_open);
        tvFilterProgress = view.findViewById(R.id.tv_filter_progress);
        tvFilterClosed = view.findViewById(R.id.tv_filter_closed);

        rvTickets = view.findViewById(R.id.rv_tickets);
        rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TicketAdapter(allTickets);
        rvTickets.setAdapter(adapter);

        setupFilters();
        fetchTickets(null);

        return view;
    }

    private void setupFilters() {
        tvFilterAll.setOnClickListener(v -> {
            currentStatus = null;
            updateFilterUI(tvFilterAll);
            fetchTickets(null);
        });

        tvFilterOpen.setOnClickListener(v -> {
            currentStatus = "OPEN";
            updateFilterUI(tvFilterOpen);
            fetchTickets("OPEN");
        });

        tvFilterProgress.setOnClickListener(v -> {
            currentStatus = "IN_PROGRESS";
            updateFilterUI(tvFilterProgress);
            fetchTickets("IN_PROGRESS");
        });

        tvFilterClosed.setOnClickListener(v -> {
            currentStatus = "CLOSED";
            updateFilterUI(tvFilterClosed);
            fetchTickets("CLOSED");
        });
    }

    private void updateFilterUI(TextView activeFilter) {
        // Reset all
        TextView[] filters = {tvFilterAll, tvFilterOpen, tvFilterProgress, tvFilterClosed};
        for (TextView tv : filters) {
            tv.setBackgroundResource(0);
            tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_grey));
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Set active
        activeFilter.setBackgroundResource(R.color.primary_blue);
        activeFilter.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        activeFilter.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void fetchTickets(String status) {
        ApiClient.getApiService(getContext()).getTickets(status, null, null, null, 1, 50)
                .enqueue(new Callback<ApiResponse<TicketsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TicketsResponse>> call, Response<ApiResponse<TicketsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    TicketsResponse ticketsResponse = response.body().getData();
                    if (ticketsResponse != null && ticketsResponse.getTickets() != null) {
                        allTickets.clear();
                        allTickets.addAll(ticketsResponse.getTickets());
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load tickets", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TicketsResponse>> call, Throwable t) {
                Log.e("TicketsFragment", "Error: " + t.getMessage());
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}