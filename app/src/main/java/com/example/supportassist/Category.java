package com.example.supportassist;

import com.google.gson.annotations.SerializedName;

public class Category {
    @SerializedName(value = "id", alternate = {"category_id"})
    private int id;
    
    @SerializedName(value = "name", alternate = {"category_name", "title", "label"})
    private String name;

    public Category() { }

    /** Used when rebuilding a Ticket from Room's cache, which only stores the category name. */
    public Category(String name) {
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}