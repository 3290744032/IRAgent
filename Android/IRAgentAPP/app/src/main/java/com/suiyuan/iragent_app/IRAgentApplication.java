package com.suiyuan.iragent_app;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.suiyuan.iragent_app.data.local.PreferencesManager;
import com.suiyuan.iragent_app.data.remote.NetworkClient;
import com.suiyuan.iragent_app.ui.screens.auth.AuthActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class IRAgentApplication extends Application {

    private static IRAgentApplication instance;
    private final AtomicBoolean mRedirectingToLogin = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initToken();
    }

    public static IRAgentApplication getInstance() {
        return instance;
    }

    private void initToken() {
        PreferencesManager preferencesManager = new PreferencesManager(this);
        String token = preferencesManager.getToken();
        if (token != null && !token.isEmpty()) {
            NetworkClient.setToken(token);
        }
    }

    /**
     * Global 401 handler — clear token and redirect to login.
     * Thread-safe: only fires once until login resets the flag.
     */
    public void onUnauthorized() {
        if (!mRedirectingToLogin.compareAndSet(false, true)) {
            return; // Already redirecting, prevent duplicate launches
        }

        PreferencesManager pm = new PreferencesManager(this);
        pm.clearAuth();
        NetworkClient.setToken(null);

        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /** Reset the redirect guard after successful login */
    public void resetRedirectGuard() {
        mRedirectingToLogin.set(false);
    }
}
