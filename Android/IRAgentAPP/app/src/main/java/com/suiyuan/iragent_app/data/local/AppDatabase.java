package com.suiyuan.iragent_app.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {ConversationEntity.class, MessageEntity.class, UserInfoEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ConversationDao conversationDao();
    public abstract MessageDao messageDao();
    public abstract UserInfoDao userInfoDao();
}
