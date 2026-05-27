package com.suiyuan.iragent_app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {

    private static final String PREFS_NAME = "iragent_prefs";
    private static final String TOKEN_KEY = "auth_token";
    private static final String USER_ID_KEY = "user_id";
    private static final String ACCOUNT_KEY = "account";
    private static final String VERIFICATION_UUID_KEY = "verification_uuid";
    private static final String DARK_MODE_KEY = "dark_mode";
    private static final String NOTIFICATION_ENABLED_KEY = "notification_enabled";
    private static final String AUTO_LOGIN_KEY = "auto_login";
    private static final String ONBOARDING_COMPLETED_KEY = "onboarding_completed_v3";
    private static final String EXAM_TYPE_KEY = "exam_type";
    private static final String TARGET_SCORE_KEY = "target_score";

    private final SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getToken() {
        String token = sharedPreferences.getString(TOKEN_KEY, null);
        if (token != null) {
            token = token.trim();
        }
        return token;
    }

    public void saveToken(String token) {
        if (token != null) {
            token = token.trim();
        }
        sharedPreferences.edit().putString(TOKEN_KEY, token).apply();
    }

    public long getUserId() {
        return sharedPreferences.getLong(USER_ID_KEY, -1);
    }

    public void saveUserId(long userId) {
        sharedPreferences.edit().putLong(USER_ID_KEY, userId).apply();
    }

    public String getAccount() {
        return sharedPreferences.getString(ACCOUNT_KEY, null);
    }

    public void saveAccount(String account) {
        sharedPreferences.edit().putString(ACCOUNT_KEY, account).apply();
    }

    public void saveUserInfo(long userId, String account) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(USER_ID_KEY, userId);
        editor.putString(ACCOUNT_KEY, account);
        editor.apply();
    }

    public String getVerificationUuid() {
        return sharedPreferences.getString(VERIFICATION_UUID_KEY, null);
    }

    public void saveVerificationUuid(String uuid) {
        sharedPreferences.edit().putString(VERIFICATION_UUID_KEY, uuid).apply();
    }

    public boolean isDarkModeEnabled() {
        return sharedPreferences.getBoolean(DARK_MODE_KEY, false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(DARK_MODE_KEY, enabled).apply();
    }

    public boolean isNotificationEnabled() {
        return sharedPreferences.getBoolean(NOTIFICATION_ENABLED_KEY, true);
    }

    public void setNotificationEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(NOTIFICATION_ENABLED_KEY, enabled).apply();
    }

    public boolean isAutoLoginEnabled() {
        return sharedPreferences.getBoolean(AUTO_LOGIN_KEY, true);
    }

    public void setAutoLoginEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(AUTO_LOGIN_KEY, enabled).apply();
    }

    public boolean isOnboardingCompleted() {
        return sharedPreferences.getBoolean(ONBOARDING_COMPLETED_KEY, false);
    }

    public void setOnboardingCompleted(boolean completed) {
        sharedPreferences.edit().putBoolean(ONBOARDING_COMPLETED_KEY, completed).apply();
    }

    public String getExamType() {
        return sharedPreferences.getString(EXAM_TYPE_KEY, "");
    }

    public void setExamType(String examType) {
        sharedPreferences.edit().putString(EXAM_TYPE_KEY, examType).apply();
    }

    public int getTargetScore() {
        return sharedPreferences.getInt(TARGET_SCORE_KEY, 0);
    }

    public void setTargetScore(int score) {
        sharedPreferences.edit().putInt(TARGET_SCORE_KEY, score).apply();
    }

    public void clearAuth() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(TOKEN_KEY);
        editor.remove(USER_ID_KEY);
        editor.remove(ACCOUNT_KEY);
        editor.remove(VERIFICATION_UUID_KEY);
        editor.apply();
    }

    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }
}
