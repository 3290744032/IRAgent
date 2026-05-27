package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class UpdateConversationRequest {
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("status")
    private String status;

    public UpdateConversationRequest(String name, String description, String status) {
        this.name = name;
        this.description = description;
        this.status = status;
    }
}
