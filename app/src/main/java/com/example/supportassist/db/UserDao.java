package com.example.supportassist.db;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserEntity> users);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity user);

    @Query("SELECT * FROM users WHERE userType = :type ORDER BY name ASC")
    PagingSource<Integer, UserEntity> getUsersPagingSource(String type);

    @Query("SELECT * FROM users WHERE userType = :type ORDER BY name ASC")
    Flowable<List<UserEntity>> getUsersFlowable(String type);

    @Query("SELECT * FROM users WHERE id = :id")
    Flowable<UserEntity> getUserById(String id);

    @Query("DELETE FROM users WHERE userType = :type")
    void clearUsersByType(String type);

    @Query("DELETE FROM users")
    void clearAll();
}
