package com.suiyuan.iragent_app.data.local;

import android.content.Context;
import androidx.room.Room;

public class Database {
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "iragent_database"
            ).build();
        }
        return instance;
    }
}
