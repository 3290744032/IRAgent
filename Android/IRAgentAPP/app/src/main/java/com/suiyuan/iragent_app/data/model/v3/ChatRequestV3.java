package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class ChatRequestV3 {
    @SerializedName("question")
    private String question;
    @SerializedName("conversation_id")
    private String conversationId;

    public ChatRequestV3(String question) {
        this.question = question;
    }

    public ChatRequestV3(String question, String conversationId) {
        this.question = question;
        this.conversationId = conversationId;
    }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
}
