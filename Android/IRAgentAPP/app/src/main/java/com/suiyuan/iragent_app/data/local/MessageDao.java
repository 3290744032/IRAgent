package com.suiyuan.iragent_app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    LiveData<List<MessageEntity>> getMessagesByConversation(String conversationId);

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    List<MessageEntity> getMessagesByConversationSync(String conversationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<MessageEntity> messages);

    @Query("DELETE FROM messages WHERE messageId = :id")
    void deleteMessage(long id);

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    void deleteMessagesByConversation(String conversationId);

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :conversationId AND status = 'failed'")
    int getFailedMessageCount(String conversationId);
}
