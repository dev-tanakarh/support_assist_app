package com.example.supportassist.db;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.supportassist.Alert;
import java.util.List;

@Dao
public interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AlertEntity> alerts);

    @Query("SELECT * FROM alerts ORDER BY id DESC")
    PagingSource<Integer, Alert> getAlertsPagingSource();

    @Query("SELECT * FROM alerts ORDER BY id DESC")
    List<AlertEntity> getAllAlerts();

    @Query("DELETE FROM alerts")
    void clearAll();

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :alertId")
    void markAsRead(int alertId);
}
