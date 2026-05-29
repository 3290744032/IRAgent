package com.suiyuan.iragent.service;

import java.util.List;

public interface PaperStrategyService {

    record DifficultySplit(int easy, int medium, int hard) {}

    record PaperConfig(List<String> knowledgePoints, DifficultySplit split, int[] typeRatios) {}

    PaperConfig computeConfig(long userId, String subject, List<String> givenKps, int questionCount);

    int[] getTypeRatios();
}
