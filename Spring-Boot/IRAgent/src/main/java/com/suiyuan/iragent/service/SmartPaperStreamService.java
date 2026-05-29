package com.suiyuan.iragent.service;

import java.util.Map;
import java.util.function.Consumer;

public interface SmartPaperStreamService {

    void streamGeneratePaper(long userId, String prompt,
                             Consumer<String> onChunk,
                             Consumer<Map<String, Object>> onComplete,
                             Consumer<Throwable> onError);
}
