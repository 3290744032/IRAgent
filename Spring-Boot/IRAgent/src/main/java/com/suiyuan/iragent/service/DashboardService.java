package com.suiyuan.iragent.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    Map<String, Object> getOverview(long userId);

    Map<String, Object> getWeeklyReport(long userId);

    Map<String, Object> getMasteryRadar(long userId);

    List<Map<String, Object>> getTodayTasks(long userId);
}
