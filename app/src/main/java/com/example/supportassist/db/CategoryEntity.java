package com.example.supportassist.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey
    public int id;
    public String name;

    public CategoryEntity(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
