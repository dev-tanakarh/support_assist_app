package com.example.supportassist.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MessageEntity> messages);

    @Query("SELECT * FROM messages WHERE ticketId = :ticketId ORDER BY createdAt ASC")
    Flowable<List<MessageEntity>> getMessagesForTicket(String ticketId);

    @Query("DELETE FROM messages WHERE ticketId = :ticketId")
    void clearMessagesForTicket(String ticketId);

    @Query("DELETE FROM messages")
    void clearAll();
}
