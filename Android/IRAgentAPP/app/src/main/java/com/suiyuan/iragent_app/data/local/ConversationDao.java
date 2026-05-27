package com.suiyuan.iragent_app.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<ConversationEntity>> getAllConversations();

    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, updatedAt DESC")
    List<ConversationEntity> getAllConversationsSync();

    @Query("SELECT * FROM conversations WHERE conversationId = :id")
    ConversationEntity getConversationById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConversation(ConversationEntity conversation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertConversations(List<ConversationEntity> conversations);

    @Update
    void updateConversation(ConversationEntity conversation);

    @Query("DELETE FROM conversations WHERE conversationId = :id")
    void deleteConversation(String id);

    @Query("UPDATE conversations SET isPinned = :isPinned WHERE conversationId = :id")
    void updatePinnedStatus(String id, boolean isPinned);

    @Query("UPDATE conversations SET unreadCount = :count WHERE conversationId = :id")
    void updateUnreadCount(String id, int count);

    @Query("DELETE FROM conversations")
    void deleteAllConversations();
}
