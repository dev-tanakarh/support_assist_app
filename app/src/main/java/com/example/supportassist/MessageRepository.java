package com.example.supportassist;

import android.util.Log;
import com.example.supportassist.db.MessageDao;
import com.example.supportassist.db.MessageEntity;
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
public class MessageRepository {
    private static final String TAG = "MessageRepository";
    private final ApiService apiService;
    private final MessageDao messageDao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Inject
    public MessageRepository(ApiService apiService, MessageDao messageDao) {
        this.apiService = apiService;
        this.messageDao = messageDao;
    }

    public Flowable<List<Message>> getMessagesForTicket(String ticketId) {
        return messageDao.getMessagesForTicket(ticketId)
                .map(entities -> {
                    List<Message> messages = new ArrayList<>();
                    for (MessageEntity e : entities) {
                        Message m = new Message();
                        m.setId(e.id);
                        m.setBody(e.body);
                        m.setInternal(e.isInternal);
                        m.setCreatedAt(e.createdAt);
                        
                        // Properly reconstruct the User object to enable correct UI alignment (isMe check)
                        User author = new User(e.authorName);
                        author.setId(e.authorId);
                        author.setUserType(e.authorRole);
                        m.setUser(author);

                        messages.add(m);
                    }
                    return messages;
                })
                .subscribeOn(Schedulers.io());
    }

    public void syncMessages(String ticketId) {
        apiService.getTicketMessages(ticketId).enqueue(new Callback<ApiResponse<List<Message>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Message>>> call, Response<ApiResponse<List<Message>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Message> messages = response.body().getData();
                    if (messages != null) {
                        saveMessagesLocally(ticketId, messages);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Message>>> call, Throwable t) {
                Log.e(TAG, "Failed to sync messages", t);
            }
        });
    }

    private void saveMessagesLocally(String ticketId, List<Message> messages) {
        List<MessageEntity> entities = new ArrayList<>();
        for (Message m : messages) {
            String authorName = m.getUser() != null ? m.getUser().getName() : "System";
            String authorId = m.getUser() != null ? m.getUser().getId() : null;
            String authorRole = m.getUser() != null ? m.getUser().getUserType() : null;
            
            entities.add(new MessageEntity(
                    m.getId(), ticketId, m.getBody(), authorName, authorId, 
                    authorRole, m.isInternal(), m.getCreatedAt()
            ));
        }
        dbExecutor.execute(() -> {
            messageDao.clearMessagesForTicket(ticketId);
            messageDao.insertAll(entities);
        });
    }
}
