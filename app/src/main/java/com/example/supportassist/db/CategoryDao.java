package com.example.supportassist.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoryEntity> categories);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    Flowable<List<CategoryEntity>> getAllCategories();

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<CategoryEntity> getAllCategoriesSync();

    @Query("DELETE FROM categories")
    void clearAll();
}
