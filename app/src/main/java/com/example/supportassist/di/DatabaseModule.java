package com.example.supportassist.di;

import android.content.Context;
import com.example.supportassist.db.AlertDao;
import com.example.supportassist.db.AppDatabase;
import com.example.supportassist.db.CategoryDao;
import com.example.supportassist.db.MessageDao;
import com.example.supportassist.db.TicketDao;
import com.example.supportassist.db.UserDao;
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
        return AppDatabase.getInstance(context);
    }

    @Provides
    public TicketDao provideTicketDao(AppDatabase database) {
        return database.ticketDao();
    }

    @Provides
    public AlertDao provideAlertDao(AppDatabase database) {
        return database.alertDao();
    }

    @Provides
    public CategoryDao provideCategoryDao(AppDatabase database) {
        return database.categoryDao();
    }

    @Provides
    public MessageDao provideMessageDao(AppDatabase database) {
        return database.messageDao();
    }

    @Provides
    public UserDao provideUserDao(AppDatabase database) {
        return database.userDao();
    }
}
