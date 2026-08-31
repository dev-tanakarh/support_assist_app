package com.example.supportassist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        RecyclerView rvRecentTickets = view.findViewById(R.id.rv_recent_tickets);
        rvRecentTickets.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Ticket> recentTickets = new ArrayList<>();
        recentTickets.add(new Ticket("#TKT-2025-0152", "Internet not connecting", "Open", "10 mins ago", "High"));
        recentTickets.add(new Ticket("#TKT-2025-0148", "Laptop overheating", "In Progress", "2h ago", "Medium"));
        recentTickets.add(new Ticket("#TKT-2025-0141", "Printer not responding", "Resolved", "1 day ago", "Low"));

        TicketAdapter adapter = new TicketAdapter(recentTickets);
        rvRecentTickets.setAdapter(adapter);

        return view;
    }
}