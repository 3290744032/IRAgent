package com.suiyuan.iragent_app.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserInfoDao {
    @Query("SELECT * FROM user_info WHERE id = 1")
    UserInfoEntity getUserInfo();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserInfo(UserInfoEntity userInfo);

    @Query("DELETE FROM user_info")
    void deleteUserInfo();
}
