package com.suiyuan.iragent_app.util;

import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class TtsRequest {

    @SerializedName("app")
    private App app;

    @SerializedName("user")
    private User user;

    @SerializedName("audio")
    private Audio audio;

    @SerializedName("request")
    private Request request;

    public TtsRequest(String text) {
        this.app = new App();
        this.user = new User();
        this.audio = new Audio();
        this.request = new Request(text);
    }

    public static class App {
        @SerializedName("cluster")
        private String cluster = "volcano_tts";
    }

    public static class User {
        @SerializedName("uid")
        private String uid = "\u8c46\u5305\u8bed\u97f3";
    }

    public static class Audio {
        @SerializedName("voice_type")
        private String voiceType = "BV001";

        @SerializedName("encoding")
        private String encoding = "mp3";

        @SerializedName("speed_ratio")
        private double speedRatio = 1.0;

        @SerializedName("volume_ratio")
        private double volumeRatio = 1.0;

        @SerializedName("pitch_ratio")
        private double pitchRatio = 1.0;
    }

    public static class Request {
        @SerializedName("reqid")
        private String reqId;

        @SerializedName("text")
        private String text;

        @SerializedName("operation")
        private String operation = "query";

        public Request(String text) {
            this.reqId = UUID.randomUUID().toString().replace("-", "");
            this.text = text;
        }
    }
}
