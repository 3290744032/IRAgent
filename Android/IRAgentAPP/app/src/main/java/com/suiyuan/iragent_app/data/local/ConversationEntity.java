package com.suiyuan.iragent_app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversations")
public class ConversationEntity {
    @NonNull
    @PrimaryKey
    public String conversationId;
    public long userId;
    public String name;
    public String description;
    public String createdAt;
    public String updatedAt;
    public String status;
    public boolean isPinned;
    public int unreadCount;

    public ConversationEntity() {}

    @Ignore
    public ConversationEntity(String conversationId, long userId, String name, String description,
                             String createdAt, String updatedAt, String status, boolean isPinned, int unreadCount) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
        this.isPinned = isPinned;
        this.unreadCount = unreadCount;
    }

    public String getId() { return conversationId; }
    public String getConversationId() { return conversationId; }
    public long getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getStatus() { return status; }
}