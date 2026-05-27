package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateConversationRequest {
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;

    public CreateConversationRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
