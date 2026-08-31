package com.example.supportassist;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AlertsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        ImageButton btnBack = view.findViewById(R.id.btn_back_alerts);
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        RecyclerView rvAlerts = view.findViewById(R.id.rv_alerts);
        rvAlerts.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Alert> alerts = new ArrayList<>();
        alerts.add(new Alert("Your ticket TKT-2025-0152 has been assigned to Technician John.", "2 mins ago", R.drawable.ic_alerts, Color.parseColor("#3B82F6")));
        alerts.add(new Alert("Your ticket TKT-2025-0148 status changed to In Progress.", "2 hours ago", R.drawable.ic_tickets, Color.parseColor("#F59E0B")));
        alerts.add(new Alert("Your ticket TKT-2025-0141 has been resolved.", "1 day ago", R.drawable.ic_launcher_foreground, Color.parseColor("#10B981")));
        alerts.add(new Alert("System Maintenance Scheduled on 10 Aug 2025", "2 days ago", R.drawable.ic_logo, Color.parseColor("#8B5CF6")));
        alerts.add(new Alert("New Knowledge Base article added: Fixing WiFi issues", "3 days ago", R.drawable.ic_other, Color.parseColor("#F97316")));

        AlertAdapter adapter = new AlertAdapter(alerts);
        rvAlerts.setAdapter(adapter);

        return view;
    }
}