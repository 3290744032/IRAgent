package com.suiyuan.iragent.service;

import java.util.Map;

public interface DailyPracticeService {

    Map<String, Object> generatePractice(long userId, String subject, int count, String knowledgePoints);

    Map<String, Object> submitAnswers(long userId, Map<String, Object> body);
}
