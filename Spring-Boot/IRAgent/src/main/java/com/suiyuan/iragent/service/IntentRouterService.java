package com.suiyuan.iragent.service;

import java.util.Map;

public interface IntentRouterService {
    enum Intent {
        HINT_NEEDED, FULL_EXPLANATION, NOTE_SEARCH, PRACTICE_READY
    }

    Intent detect(String userMessage);
    Map<String, Object> getModeConfig(Intent intent);
}
