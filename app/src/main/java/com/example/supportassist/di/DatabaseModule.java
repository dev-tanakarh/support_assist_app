package com.example.supportassist.di;

import android.content.Context;
import androidx.room.Room;
import com.example.supportassist.db.AppDatabase;
import com.example.supportassist.db.TicketDao;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context,
                AppDatabase.class,
                "support_assist_db"
        ).fallbackToDestructiveMigration().build();
    }

    @Provides
    public TicketDao provideTicketDao(AppDatabase database) {
        return database.ticketDao();
    }
}
