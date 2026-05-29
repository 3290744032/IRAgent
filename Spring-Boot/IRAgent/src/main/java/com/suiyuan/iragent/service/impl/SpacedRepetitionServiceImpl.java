package com.suiyuan.iragent.service.impl;

import com.suiyuan.iragent.service.SpacedRepetitionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SpacedRepetitionServiceImpl implements SpacedRepetitionService {

    private static final int[] INTERVALS_DAYS = {0, 1, 3, 7, 15, 30};

    public int getMaxLevel() {
        return INTERVALS_DAYS.length;
    }

    public LocalDateTime nextReviewTime(int currentLevel) {
        int days = currentLevel < INTERVALS_DAYS.length
                ? INTERVALS_DAYS[currentLevel] : 365;
        return LocalDateTime.now().plusDays(days);
    }

    public boolean isMastered(int level) {
        return level >= INTERVALS_DAYS.length;
    }
}
