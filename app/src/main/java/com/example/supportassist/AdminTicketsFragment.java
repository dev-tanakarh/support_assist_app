package com.example.supportassist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class AdminTicketsFragment extends Fragment {

    @Inject ApiService apiService;

    private ProgressBar progressBar;
    private RecyclerView rvTickets;
    private TextView tvEmpty;
    private Spinner spStatus;
    private TicketAdapter adapter;
    private String[] statusValues;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_tickets, container, false);

        progressBar = view.findViewById(R.id.loading_indicator);
        rvTickets = view.findViewById(R.id.rv_tickets);
        tvEmpty = view.findViewById(R.id.tv_empty);
        spStatus = view.findViewById(R.id.sp_status_filter);

        adapter = new TicketAdapter(new ArrayList<>());
        rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTickets.setAdapter(adapter);

        statusValues = getResources().getStringArray(R.array.status_filter_values);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.status_filter_labels, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(spinnerAdapter);
        spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                loadTickets(statusValues[position].isEmpty() ? null : statusValues[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        int pos = spStatus.getSelectedItemPosition();
        loadTickets(pos >= 0 && !statusValues[pos].isEmpty() ? statusValues[pos] : null);
    }

    private void loadTickets(String status) {
        progressBar.setVisibility(View.VISIBLE);
        apiService.getTicketsList(status, null, null, 1, 100).enqueue(new Callback<ApiResponse<TicketsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TicketsResponse>> call, Response<ApiResponse<TicketsResponse>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.body() == null || !response.body().isSuccess()) return;
                List<Ticket> tickets = response.body().getData().getTickets();
                if (tickets == null) tickets = new ArrayList<>();
                adapter.updateTickets(tickets);
                tvEmpty.setVisibility(tickets.isEmpty() ? View.VISIBLE : View.GONE);
                rvTickets.setVisibility(tickets.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onFailure(Call<ApiResponse<TicketsResponse>> call, Throwable t) {
                if (isAdded()) progressBar.setVisibility(View.GONE);
            }
        });
    }
}
