package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class SolveResponse {
    @SerializedName("solution")
    private String solution;
    
    @SerializedName("segments")
    private List<ResponseSegment> segments;
    
    @SerializedName("userId")
    private long userId;
    
    @SerializedName("conversationId")
    private String conversationId;

    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    
    public List<ResponseSegment> getSegments() { return segments; }
    public void setSegments(List<ResponseSegment> segments) { this.segments = segments; }
    
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public static String cleanMarkdownContent(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        int startIdx = -1;
        
        int itemOutputIdx = raw.indexOf("ItemOutputMessage");
        if (itemOutputIdx != -1) {
            int textIdx = raw.indexOf("text='", itemOutputIdx);
            if (textIdx != -1) {
                startIdx = textIdx + 6;
            }
        }

        if (startIdx == -1) {
            startIdx = raw.indexOf("###");
        }

        if (startIdx == -1) {
            startIdx = raw.indexOf("<PLOT>");
        }

        if (startIdx == -1) {
            startIdx = raw.indexOf("【PLOT】");
        }

        if (startIdx == -1) {
            return raw.trim();
        }

        String content = raw.substring(startIdx);

        int minEndIdx = content.length();
        
        String[] endMarkers = {
            "' annotations=null}]",
            ", annotations=null}]",
            "', annotations=null}]",
            "', annotations=null]",
            ", annotations=null]",
            "status='completed'",
            "id='msg_",
            "partial=null}]",
            "previousResponseId=",
            "thinking=null",
            "reasoning=null",
            "serviceTier=",
            "temperature=null",
            "tools=null",
            "topP=null",
            "usage=Usage",
            "inputTokens=",
            "outputTokens="
        };
        
        for (String marker : endMarkers) {
            int idx = content.indexOf(marker);
            if (idx != -1 && idx < minEndIdx) {
                minEndIdx = idx;
            }
        }

        content = content.substring(0, minEndIdx);

        content = content.replace("\\'", "'");
        content = content.replace("\\\"", "\"");
        content = content.replace("\\\\n", "\\n");
        content = content.replace("\\n", "\n");
        content = content.replace("\\r", "");
        content = content.replace("\\\\", "\\");

        content = content.trim();
        
        if (content.endsWith("'")) {
            content = content.substring(0, content.length() - 1);
        }
        if (content.endsWith("'))")) {
            content = content.substring(0, content.length() - 3);
        }
        
        return content.trim();
    }

    private static String convertLatexToPlainText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        text = text.replace("\\pi", "π");
        text = text.replace("\\in", "∈");
        text = text.replace("\\infty", "∞");
        text = text.replace("\\approx", "≈");
        text = text.replace("\\to", "→");

        return text;
    }

    public String extractCleanMarkdownContent() {
        return cleanMarkdownContent(solution);
    }

    public List<ResponseSegment> parseSolutionToSegments() {
        List<ResponseSegment> result = new ArrayList<>();
        
        if (segments != null && !segments.isEmpty()) {
            return segments;
        }
        
        String cleanText = extractCleanMarkdownContent();
        
        if (cleanText.isEmpty()) {
            return result;
        }

        ResponseSegment textSegment = new ResponseSegment();
        textSegment.setType("text");
        textSegment.setContent(cleanText);
        result.add(textSegment);
        
        return result;
    }
}