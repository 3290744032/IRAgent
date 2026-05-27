package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("account")
    private String account;
    @SerializedName("password")
    private String password;
    @SerializedName("verifiCode")
    private String verifiCode;
    @SerializedName("uuid")
    private String uuid;

    public LoginRequest(String account, String password, String verifiCode, String uuid) {
        this.account = account;
        this.password = password;
        this.verifiCode = verifiCode;
        this.uuid = uuid;
    }
}
