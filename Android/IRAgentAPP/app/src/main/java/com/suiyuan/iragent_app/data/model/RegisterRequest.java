package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    @SerializedName("account")
    private String account;
    @SerializedName("password")
    private String password;
    @SerializedName("email")
    private String email;
    @SerializedName("telphone")
    private String telphone;
    @SerializedName("verifiCode")
    private String verifiCode;
    @SerializedName("uuid")
    private String uuid;

    public RegisterRequest(String account, String password, String email, String telphone, String verifiCode, String uuid) {
        this.account = account;
        this.password = password;
        this.email = email;
        this.telphone = telphone;
        this.verifiCode = verifiCode;
        this.uuid = uuid;
    }
}
