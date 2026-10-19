package com.example.supportassist;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LoadState;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supportassist.viewmodel.AlertViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

@AndroidEntryPoint
public class AlertsFragment extends Fragment {

    private RecyclerView rvAlerts;
    private AlertAdapter adapter;
    private ProgressBar progressBar;
    private View emptyState;
    private AlertViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        viewModel = new ViewModelProvider(this).get(AlertViewModel.class);

        progressBar = view.findViewById(R.id.loading_indicator);
        emptyState = view.findViewById(R.id.ll_empty_state); // Assuming this ID exists or adding it if missing

        ImageButton btnBack = view.findViewById(R.id.btn_back_alerts);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        rvAlerts = view.findViewById(R.id.rv_alerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AlertAdapter();
        rvAlerts.setAdapter(adapter);

        adapter.setOnAlertClickListener((alert, position) -> {
            showAlertDetails(alert);
        });

        adapter.addLoadStateListener(loadStates -> {
            boolean isLoading = loadStates.getRefresh() instanceof LoadState.Loading;
            if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            
            boolean isEmpty = loadStates.getRefresh() instanceof LoadState.NotLoading && adapter.getItemCount() == 0;
            if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            
            return null;
        });

        loadAlerts();
        
        // Trigger a sync
        viewModel.syncAlerts(1, 20);

        return view;
    }

    private void loadAlerts() {
        disposables.add(viewModel.getAlertsPaging()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pagingData -> adapter.submitData(getLifecycle(), pagingData)));
    }

    private void showAlertDetails(Alert alert) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_alert_details, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        TextView tvTitle = dialogView.findViewById(R.id.tv_alert_detail_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_alert_detail_message);
        TextView tvTime = dialogView.findViewById(R.id.tv_alert_detail_time);
        MaterialButton btnMarkRead = dialogView.findViewById(R.id.btn_mark_read);
        MaterialButton btnViewTicket = dialogView.findViewById(R.id.btn_view_ticket);
        MaterialButton btnClose = dialogView.findViewById(R.id.btn_close_dialog);

        tvTitle.setText(alert.getTitle());
        tvMessage.setText(alert.getMessage());
        tvTime.setText(alert.getCreatedAt());

        if (alert.isRead()) {
            btnMarkRead.setVisibility(View.GONE);
        } else {
            btnMarkRead.setOnClickListener(v -> {
                viewModel.markAsRead(alert.getId());
                dialog.dismiss();
            });
        }

        if (alert.getTicketId() != null && !alert.getTicketId().isEmpty()) {
            btnViewTicket.setVisibility(View.VISIBLE);
            btnViewTicket.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), TicketDetailsActivity.class);
                intent.putExtra("ticket_id", alert.getTicketId());
                startActivity(intent);
                dialog.dismiss();
            });
        } else {
            btnViewTicket.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.dispose();
    }
}
