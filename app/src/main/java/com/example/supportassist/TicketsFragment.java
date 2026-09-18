package com.example.supportassist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supportassist.paging.TicketPagingAdapter;
import com.example.supportassist.viewmodel.TicketViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class TicketsFragment extends Fragment {

    private RecyclerView rvTickets;
    private TicketPagingAdapter pagingAdapter;
    private TextView tvFilterAll, tvFilterOpen, tvFilterProgress, tvFilterClosed;
    private TicketViewModel viewModel;
    private final CompositeDisposable disposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tickets, container, false);

        viewModel = new ViewModelProvider(this).get(TicketViewModel.class);

        tvFilterAll = view.findViewById(R.id.tv_filter_all);
        tvFilterOpen = view.findViewById(R.id.tv_filter_open);
        tvFilterProgress = view.findViewById(R.id.tv_filter_progress);
        tvFilterClosed = view.findViewById(R.id.tv_filter_closed);

        rvTickets = view.findViewById(R.id.rv_tickets);
        rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // 3. Performance: Paging 3 Integration
        pagingAdapter = new TicketPagingAdapter();
        rvTickets.setAdapter(pagingAdapter);

        setupFilters();
        loadTickets(null);

        return view;
    }

    private void loadTickets(String status) {
        disposable.clear();
        disposable.add(viewModel.getTicketsPaging(status).subscribe(pagingData -> {
            pagingAdapter.submitData(getLifecycle(), pagingData);
        }));
    }

    private void setupFilters() {
        tvFilterAll.setOnClickListener(v -> {
            updateFilterUI(tvFilterAll);
            loadTickets(null);
        });
        tvFilterOpen.setOnClickListener(v -> {
            updateFilterUI(tvFilterOpen);
            loadTickets("OPEN");
        });
        tvFilterProgress.setOnClickListener(v -> {
            updateFilterUI(tvFilterProgress);
            loadTickets("IN_PROGRESS");
        });
        tvFilterClosed.setOnClickListener(v -> {
            updateFilterUI(tvFilterClosed);
            loadTickets("CLOSED");
        });
    }

    private void updateFilterUI(TextView activeFilter) {
        TextView[] filters = {tvFilterAll, tvFilterOpen, tvFilterProgress, tvFilterClosed};
        for (TextView tv : filters) {
            tv.setBackgroundResource(0);
            tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_grey));
        }
        activeFilter.setBackgroundResource(R.color.primary_blue);
        activeFilter.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposable.dispose();
    }
}
