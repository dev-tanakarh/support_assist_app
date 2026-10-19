package com.example.supportassist.db;

import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface TicketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TicketEntity> tickets);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TicketEntity ticket);

    @Query("SELECT * FROM tickets ORDER BY id DESC")
    List<TicketEntity> getAllTickets();

    @Query("SELECT * FROM tickets WHERE (:isStaff = 1) OR (reporterId = :userId) ORDER BY id DESC LIMIT 50")
    Flowable<List<TicketEntity>> getRecentTicketsFiltered(String userId, boolean isStaff);

    @Query("SELECT * FROM tickets ORDER BY id DESC LIMIT 50")
    Flowable<List<TicketEntity>> getRecentTicketsFlowable();

    @Query("SELECT * FROM tickets WHERE id = :id")
    Flowable<TicketEntity> getTicketById(String id);

    @Query("SELECT * FROM tickets ORDER BY id DESC")
    PagingSource<Integer, TicketEntity> getTicketsPagingSource();

    @Query("DELETE FROM tickets")
    void clearAll();
}
