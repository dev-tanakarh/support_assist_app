package com.example.supportassist;

public class Ticket {
    private String id;
    private String subject;
    private String status;
    private String time;
    private String priority;

    public Ticket(String id, String subject, String status, String time, String priority) {
        this.id = id;
        this.subject = subject;
        this.status = status;
        this.time = time;
        this.priority = priority;
    }

    public String getId() { return id; }
    public String getSubject() { return subject; }
    public String getStatus() { return status; }
    public String getTime() { return time; }
    public String getPriority() { return priority; }
}