package com.suiyuan.iragent.service;

import java.util.Map;

public interface FeedbackTokenService {
    
    String generateToken(String sessionId, Integer stepId, String userAnswer);
    
    Map<String, Object> getTokenData(String token);
    
    boolean validateToken(String token);
    
    void removeToken(String token);
    
    void setTokenData(String token, Map<String, Object> data, long expireSeconds);
}
