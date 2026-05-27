package com.suiyuan.iragent_app.data.local;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_info")
public class UserInfoEntity {
    @PrimaryKey
    public long id;
    public long userId;
    public String account;
    public String email;
    public String telphone;

    public UserInfoEntity() {}

    @Ignore
    public UserInfoEntity(long id, long userId, String account, String email, String telphone) {
        this.id = id;
        this.userId = userId;
        this.account = account;
        this.email = email;
        this.telphone = telphone;
    }
}
