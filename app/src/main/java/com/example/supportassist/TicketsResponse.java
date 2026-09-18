package com.example.supportassist;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TicketsResponse {
    @SerializedName("tickets")
    private List<Ticket> tickets;
    
    @SerializedName("pagination")
    private Pagination pagination;

    public List<Ticket> getTickets() { return tickets; }
    public Pagination getPagination() { return pagination; }

    public static class Pagination {
        private int page;
        private int perPage;
        private int total;
        private int totalPages;

        public int getPage() { return page; }
        public int getPerPage() { return perPage; }
        public int getTotal() { return total; }
        public int getTotalPages() { return totalPages; }
    }
}