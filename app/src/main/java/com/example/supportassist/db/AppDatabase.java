package com.example.supportassist.db;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TicketEntity.class, AlertEntity.class, CategoryEntity.class, MessageEntity.class, UserEntity.class}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "support_assist_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public abstract TicketDao ticketDao();
    public abstract AlertDao alertDao();
    public abstract CategoryDao categoryDao();
    public abstract MessageDao messageDao();
    public abstract UserDao userDao();
}
