package com.suiyuan.iragent.service;

import java.time.LocalDateTime;

public interface SpacedRepetitionService {

    int getMaxLevel();

    LocalDateTime nextReviewTime(int currentLevel);

    boolean isMastered(int level);
}
