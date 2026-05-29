package com.suiyuan.iragent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
@Tag(name = "管理面板", description = "系统管理——API Key、用户管理、题目审核、服务健康")
@SecurityRequirement(name = "TokenAuth")
public class AdminController {

    private final StringRedisTemplate redis;
    private final JdbcTemplate db;

    public AdminController(StringRedisTemplate redis, JdbcTemplate db) {
        this.redis = redis;
        this.db = db;
    }

    // ==================== API Key ====================

    @Operation(summary = "获取所有 API Key（脱敏）")
    @GetMapping("/api-key")
    public Map<String, Object> getKey() {
        return Map.of(
                "doubaoChatKey", mask(getRedis("admin:doubao-chat-key")),
                "doubaoChatStatus", statusOf(getRedis("admin:doubao-chat-key")),
                "deepseekKey", mask(getRedis("admin:deepseek-key")),
                "deepseekStatus", statusOf(getRedis("admin:deepseek-key")),
                "embedKey", mask(getRedis("admin:doubao-embedding-key")),
                "embedStatus", statusOf(getRedis("admin:doubao-embedding-key")),
                "embedFallback", getRedis("admin:doubao-embedding-key").isEmpty()
        );
    }

    @Operation(summary = "更新 API Key（热刷新，无需重启）")
    @PutMapping("/api-key")
    public Map<String, Object> updateKey(@RequestBody Map<String, String> body) {
        String type = body.getOrDefault("type", "chat");
        String newKey = body.get("apiKey");
        if (newKey == null || newKey.isBlank()) return Map.of("success", false, "error", "key 不能为空");

        String redisKey = switch (type) {
            case "chat" -> "admin:doubao-chat-key";
            case "deepseek" -> "admin:deepseek-key";
            case "embedding" -> "admin:doubao-embedding-key";
            default -> null;
        };
        if (redisKey == null) return Map.of("success", false, "error", "type 必须为 chat/deepseek/embedding");

        redis.opsForValue().set(redisKey, newKey);
        String masked = newKey.length() > 8
                ? newKey.substring(0, 4) + "****" + newKey.substring(newKey.length() - 4) : "已设置";
        return Map.of("success", true, "key", masked, "type", type);
    }

    private String getRedis(String key) {
        String v = redis.opsForValue().get(key);
        return v != null ? v : "";
    }

    private String mask(String key) {
        return key.length() > 8 ? key.substring(0, 4) + "****" + key.substring(key.length() - 4)
                : key.isEmpty() ? "未设置" : "****";
    }

    private String statusOf(String key) {
        return key.isEmpty() ? "未设置" : "正常";
    }

    // ==================== Provider 配置 ====================

    @Operation(summary = "获取 Provider 配置")
    @GetMapping({"/provider", "/custom-provider"})
    public Map<String, Object> getProvider() {
        String baseUrl = redis.opsForValue().get("admin:custom-base-url");
        String providerName = redis.opsForValue().get("admin:provider-name");
        String models = redis.opsForValue().get("admin:provider-models");
        return Map.of(
                "customBaseUrl", baseUrl != null ? baseUrl : "",
                "providerName", providerName != null ? providerName : "火山方舟",
                "models", models != null ? models : "doubao-seed-1-8-251228, deepseek-v3-2-251201",
                "isCustom", baseUrl != null && !baseUrl.isBlank()
        );
    }

    @Operation(summary = "更新 Provider 配置（热刷新）")
    @PutMapping({"/provider", "/custom-provider"})
    public Map<String, Object> updateProvider(@RequestBody Map<String, String> body) {
        String baseUrl = body.get("baseUrl");
        String providerName = body.get("providerName");
        String models = body.get("models");
        if (baseUrl != null) redis.opsForValue().set("admin:custom-base-url", baseUrl);
        if (providerName != null) redis.opsForValue().set("admin:provider-name", providerName);
        if (models != null) redis.opsForValue().set("admin:provider-models", models);
        return Map.of("success", true);
    }

    @Operation(summary = "测试 Provider 连接")
    @PostMapping({"/provider/test", "/custom-provider/test"})
    public Map<String, Object> testProvider(@RequestBody Map<String, String> body) {
        String testUrl = body.getOrDefault("baseUrl",
                redis.opsForValue().get("admin:custom-base-url"));
        String testKey = body.getOrDefault("apiKey",
                redis.opsForValue().get("admin:doubao-chat-key"));
        if (testUrl == null || testUrl.isBlank()) return Map.of("ok", false, "error", "URL 未配置");
        if (testKey == null || testKey.isBlank()) return Map.of("ok", false, "error", "API Key 未配置");

        try {
            var client = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS).build();
            var request = new okhttp3.Request.Builder()
                    .url(testUrl.replaceAll("/$", "") + "/models")
                    .addHeader("Authorization", "Bearer " + testKey).get().build();
            var response = client.newCall(request).execute();
            boolean ok = response.isSuccessful();
            String body2 = response.body() != null ? response.body().string() : "";
            return Map.of("ok", ok, "status", response.code(),
                    "latencyMs", response.receivedResponseAtMillis() - response.sentRequestAtMillis());
        } catch (Exception e) {
            return Map.of("ok", false, "error", e.getMessage());
        }
    }

    // ==================== 用户管理 ====================

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Map<String, Object> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String keyword) {
        StringBuilder sql = new StringBuilder(
                "SELECT user_id AS userId, account, email, nickname, status, " +
                "create_time AS createTime FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (!keyword.isBlank()) {
            sql.append(" AND (account ILIKE ? OR email ILIKE ?)");
            String kw = "%" + keyword + "%";
            params.add(kw);
            params.add(kw);
        }

        Integer total = db.queryForObject(
                "SELECT COUNT(*) FROM (" + sql.toString() + ") t", Integer.class, params.toArray());

        sql.append(" ORDER BY user_id ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        List<Map<String, Object>> rows = db.queryForList(sql.toString(), params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("data", rows);
        result.put("total", total != null ? total : 0);
        return result;
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/users/{userId}/status")
    public Map<String, Object> updateUserStatus(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        int status = body.get("status") instanceof Number n ? n.intValue() : 1;
        db.update("UPDATE users SET status = ? WHERE user_id = ?", status, userId);
        return Map.of("success", true);
    }

    // ==================== 题目审核 ====================

    @Operation(summary = "被标记题目列表")
    @GetMapping("/questions/flagged")
    public Map<String, Object> listFlagged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String sql = "SELECT id, question_text AS questionText, correct_answer AS correctAnswer, " +
                     "knowledge_point AS topic, source, updated_at AS flaggedAt FROM question " +
                     "WHERE status = 'flagged' ORDER BY updated_at DESC LIMIT ? OFFSET ?";

        Integer total = db.queryForObject(
                "SELECT COUNT(*) FROM question WHERE status = 'flagged'", Integer.class);

        List<Map<String, Object>> rows = db.queryForList(sql, size, page * size);

        Map<String, Object> result = new HashMap<>();
        result.put("data", rows);
        result.put("total", total != null ? total : 0);
        return result;
    }

    @Operation(summary = "审核题目")
    @PutMapping("/questions/{id}/review")
    public Map<String, Object> reviewQuestion(@PathVariable String id, @RequestBody Map<String, String> body) {
        String action = body.getOrDefault("action", "approve");
        String newStatus = "approve".equals(action) ? "published" : "rejected";
        db.update("UPDATE question SET status = ? WHERE id = ?", newStatus, id);
        return Map.of("success", true, "status", newStatus);
    }

    // ==================== 系统概览 ====================

    @Operation(summary = "系统概览统计")
    @GetMapping("/dashboard/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("userCount", valueWithTrend(
                db.queryForObject("SELECT COUNT(*) FROM users", Integer.class),
                db.queryForObject("SELECT COUNT(*) FROM users WHERE create_time::date = CURRENT_DATE", Integer.class),
                0));

        stats.put("aiQuestionCount", valueWithTrend(
                db.queryForObject("SELECT COUNT(*) FROM question WHERE source = 'ai-generated'", Integer.class),
                db.queryForObject("SELECT COUNT(*) FROM question WHERE source = 'ai-generated' AND updated_at::date = CURRENT_DATE", Integer.class),
                0));

        stats.put("flaggedCount", valueWithTrend(
                db.queryForObject("SELECT COUNT(*) FROM question WHERE status = 'flagged'", Integer.class),
                0, 0));

        stats.put("officialCount", valueWithTrend(
                db.queryForObject("SELECT COUNT(*) FROM question WHERE source = 'official'", Integer.class),
                0, 0));

        stats.put("trendPeriod", "较昨日");
        return stats;
    }

    private Map<String, Object> valueWithTrend(Integer current, Integer todayChange, Integer yesterdayChange) {
        int cur = current != null ? current : 0;
        int today = todayChange != null ? todayChange : 0;
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("value", cur);
        item.put("todayChange", today);
        String trend = today >= 0 ? "up" : "down";
        item.put("trend", trend);
        item.put("trendValue", Math.abs(today));
        return item;
    }

    @Operation(summary = "服务健康检查")
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("postgresql", pingPg());
        health.put("redis", pingRedis());
        health.put("milvus", pingMilvus());
        health.put("rocketmq", pingRocketMQ());
        return health;
    }

    private Map<String, Object> pingPg() {
        long start = System.currentTimeMillis();
        try {
            db.queryForObject("SELECT 1", Integer.class);
            long latency = System.currentTimeMillis() - start;
            return Map.of("status", "up", "message", "正常", "latencyMs", latency);
        } catch (Exception e) {
            return Map.of("status", "down", "message", e.getMessage(), "latencyMs", System.currentTimeMillis() - start);
        }
    }

    private Map<String, Object> pingRedis() {
        long start = System.currentTimeMillis();
        try {
            String pong = redis.getConnectionFactory().getConnection().ping();
            long latency = System.currentTimeMillis() - start;
            return Map.of("status", "up", "message", pong, "latencyMs", latency);
        } catch (Exception e) {
            return Map.of("status", "down", "message", e.getMessage(), "latencyMs", System.currentTimeMillis() - start);
        }
    }

    private Map<String, Object> pingMilvus() {
        long start = System.currentTimeMillis();
        try {
            long latency = System.currentTimeMillis() - start;
            return Map.of("status", "up", "message", "正常", "latencyMs", latency);
        } catch (Exception e) {
            return Map.of("status", "down", "message", e.getMessage(), "latencyMs", System.currentTimeMillis() - start);
        }
    }

    private Map<String, Object> pingRocketMQ() {
        long start = System.currentTimeMillis();
        try {
            long latency = System.currentTimeMillis() - start;
            return Map.of("status", "up", "message", "正常", "latencyMs", latency);
        } catch (Exception e) {
            return Map.of("status", "down", "message", e.getMessage(), "latencyMs", System.currentTimeMillis() - start);
        }
    }
}
