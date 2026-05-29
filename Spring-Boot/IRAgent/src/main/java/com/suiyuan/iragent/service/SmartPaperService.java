package com.suiyuan.iragent.service;

import java.util.List;
import java.util.Map;

public interface SmartPaperService {

    Map<String, Object> generatePaper(long userId, String subject, String examType,
                                      String title, int questionCount, int difficulty,
                                      List<String> kps, boolean excludeDone);

    Map<String, Object> submitAnswers(long userId, Map<String, Object> body);
}
