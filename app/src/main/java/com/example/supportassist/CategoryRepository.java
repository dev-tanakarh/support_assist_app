package com.example.supportassist;

import android.util.Log;
import com.example.supportassist.db.CategoryDao;
import com.example.supportassist.db.CategoryEntity;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class CategoryRepository {
    private static final String TAG = "CategoryRepository";
    private final ApiService apiService;
    private final CategoryDao categoryDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public CategoryRepository(ApiService apiService, CategoryDao categoryDao) {
        this.apiService = apiService;
        this.categoryDao = categoryDao;
    }

    public Flowable<List<Category>> getCategories() {
        return categoryDao.getAllCategories()
                .map(entities -> {
                    List<Category> categories = new ArrayList<>();
                    for (CategoryEntity entity : entities) {
                        categories.add(new Category(entity.id, entity.name));
                    }
                    return categories;
                })
                .subscribeOn(Schedulers.io());
    }

    public void syncCategories() {
        apiService.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Category> categories = response.body().getData();
                    if (categories != null) {
                        saveCategoriesLocally(categories);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                Log.e(TAG, "Failed to sync categories", t);
            }
        });
    }

    private void saveCategoriesLocally(List<Category> categories) {
        List<CategoryEntity> entities = new ArrayList<>();
        for (Category c : categories) {
            entities.add(new CategoryEntity(c.getId(), c.getName()));
        }
        dbExecutor.execute(() -> {
            categoryDao.clearAll();
            categoryDao.insertAll(entities);
        });
    }
}
