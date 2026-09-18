package com.example.supportassist.di;

import android.content.Context;
import com.example.supportassist.ApiClient;
import com.example.supportassist.ApiService;
import com.example.supportassist.DataRepository;
import com.example.supportassist.TokenManager;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public TokenManager provideTokenManager(@ApplicationContext Context context) {
        return new TokenManager(context);
    }

    @Provides
    @Singleton
    public ApiService provideApiService(@ApplicationContext Context context) {
        return ApiClient.getApiService(context);
    }

    @Provides
    @Singleton
    public DataRepository provideDataRepository(@ApplicationContext Context context) {
        return DataRepository.getInstance(context);
    }
}
