package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SubmitAnswerRequest {
    @SerializedName("sessionId") private String sessionId;
    @SerializedName("source") private String source;
    @SerializedName("answers") private List<AnswerEntry> answers;

    public SubmitAnswerRequest(String sessionId, String source, List<AnswerEntry> answers) {
        this.sessionId = sessionId;
        this.source = source;
        this.answers = answers;
    }

    public static class AnswerEntry {
        @SerializedName("questionId") private String questionId;
        @SerializedName("selectedAnswer") private String selectedAnswer;
        @SerializedName("timeUsed") private int timeUsed;

        public AnswerEntry(String questionId, String selectedAnswer, int timeUsed) {
            this.questionId = questionId;
            this.selectedAnswer = selectedAnswer;
            this.timeUsed = timeUsed;
        }
    }
}
