package com.example.supportassist;

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

public class TicketsFragment extends Fragment {

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

        RecyclerView rvTickets = view.findViewById(R.id.rv_tickets);
        rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Ticket> allTickets = new ArrayList<>();
        allTickets.add(new Ticket("TKT-2025-0152", "WiFi keeps disconnecting", "Open", "10 mins ago", "High"));
        allTickets.add(new Ticket("TKT-2025-0148", "Laptop overheating", "In Progress", "2h ago", "Medium"));
        allTickets.add(new Ticket("TKT-2025-0141", "Printer not responding", "Resolved", "1 day ago", "Low"));
        allTickets.add(new Ticket("TKT-2025-0138", "Need Microsoft Office", "Closed", "3 days ago", "Low"));

        TicketAdapter adapter = new TicketAdapter(allTickets);
        rvTickets.setAdapter(adapter);

        return view;
    }
}