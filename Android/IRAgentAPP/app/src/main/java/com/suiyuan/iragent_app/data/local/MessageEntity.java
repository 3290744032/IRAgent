package com.suiyuan.iragent_app.data.local;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "messages",
        foreignKeys = @ForeignKey(
                entity = ConversationEntity.class,
                parentColumns = "conversationId",
                childColumns = "conversationId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("conversationId")
)
public class MessageEntity {
    @PrimaryKey
    public long messageId;
    public String conversationId;
    public String senderType;
    public String content;
    public String messageType;
    public String createdAt;
    public String status;

    public MessageEntity() {}

    @Ignore
    public MessageEntity(long messageId, String conversationId, String senderType, String content,
                         String messageType, String createdAt, String status) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderType = senderType;
        this.content = content;
        this.messageType = messageType;
        this.createdAt = createdAt;
        this.status = status;
    }
}
