package com.example.supportassist;

public class Category {
    private int id;
    private String name;

    public Category() { }

    /** Used when rebuilding a Ticket from Room's cache, which only stores the category name. */
    public Category(String name) {
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}