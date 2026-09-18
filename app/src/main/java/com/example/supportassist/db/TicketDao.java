package com.example.supportassist.db;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TicketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TicketEntity> tickets);

    @Query("SELECT * FROM tickets ORDER BY id DESC")
    List<TicketEntity> getAllTickets();

    @Query("SELECT * FROM tickets ORDER BY id DESC")
    PagingSource<Integer, TicketEntity> getTicketsPagingSource();

    @Query("DELETE FROM tickets")
    void clearAll();
}
