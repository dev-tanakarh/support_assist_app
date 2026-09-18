package com.example.supportassist.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {TicketEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract TicketDao ticketDao();
}
