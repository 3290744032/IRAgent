package com.suiyuan.iragent.dto.sse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSETeachingEvent {
    private String type;
    private Object data;
    
    public static SSETeachingEvent start() {
        return new SSETeachingEvent("start", new Object());
    }
    
    public static SSETeachingEvent content(String text) {
        return new SSETeachingEvent("content", new ContentData(text));
    }
    
    public static SSETeachingEvent formula(String latex) {
        return new SSETeachingEvent("formula", new FormulaData(latex));
    }
    
    public static SSETeachingEvent done(boolean masterAvailable) {
        return new SSETeachingEvent("done", new DoneData(masterAvailable, null));
    }
    
    public static SSETeachingEvent done(String suggestNextAction) {
        return new SSETeachingEvent("done", new DoneData(false, suggestNextAction));
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentData {
        private String text;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormulaData {
        private String latex;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoneData {
        private Boolean masterAvailable;
        private String suggestNextAction;
    }
}
