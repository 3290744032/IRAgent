package com.suiyuan.iragent_app.data.local;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class ConversationWithMessages {
    @Embedded
    public ConversationEntity conversation;
    @Relation(
            parentColumn = "conversationId",
            entityColumn = "conversationId"
    )
    public List<MessageEntity> messages;
}
