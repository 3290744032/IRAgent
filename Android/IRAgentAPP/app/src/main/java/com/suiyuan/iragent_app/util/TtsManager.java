package com.suiyuan.iragent_app.util;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

/**
 * P3 音画同步：Android 原生 TTS 管理器
 *
 * 用法：
 *   TtsManager tts = new TtsManager(context);
 *   tts.speak("actionId", "你好", 2.0, callback);
 *   tts.stop();
 *   tts.destroy();
 */
public class TtsManager {

    private static final String TAG = "TtsManager";

    private TextToSpeech tts;
    private boolean initialized = false;
    private SpeakCallback currentCallback;

    public interface SpeakCallback {
        void onStart(String actionId);
        void onDone(String actionId);
        void onError(String actionId, String error);
    }

    public TtsManager(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.CHINESE);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "TTS 中文语音包缺失，将使用默认语言");
                }
                tts.setSpeechRate(1.0f);
                tts.setPitch(1.0f);
                initialized = true;
                Log.d(TAG, "TTS 初始化成功");
            } else {
                Log.e(TAG, "TTS 初始化失败: status=" + status);
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "TTS onStart: " + utteranceId);
                if (currentCallback != null) {
                    currentCallback.onStart(utteranceId);
                }
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d(TAG, "TTS onDone: " + utteranceId);
                if (currentCallback != null) {
                    currentCallback.onDone(utteranceId);
                }
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                Log.e(TAG, "TTS onError: " + utteranceId + " code=" + errorCode);
                if (currentCallback != null) {
                    currentCallback.onError(utteranceId, "TTS error code=" + errorCode);
                }
            }

            @Deprecated
            @Override
            public void onError(String utteranceId) {
                onError(utteranceId, TextToSpeech.ERROR);
            }
        });
    }

    /**
     * 朗读文本，完成后回调
     * @param actionId 动作 ID，用于 JS 端音画同步
     * @param text 要朗读的文本
     * @param duration 预估时长（秒），用于 JS 端超时降级
     * @param callback 回调
     */
    public void speak(String actionId, String text, double duration, SpeakCallback callback) {
        if (!initialized || tts == null) {
            Log.w(TAG, "TTS 未就绪，直接回调 onDone");
            if (callback != null) callback.onDone(actionId);
            return;
        }

        this.currentCallback = callback;

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, actionId);

        int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, actionId);
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS speak 失败，回调查 done");
            if (callback != null) callback.onDone(actionId);
        }
    }

    /** 停止当前朗读 */
    public void stop() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }
    }

    /** 是否正在朗读 */
    public boolean isSpeaking() {
        return tts != null && tts.isSpeaking();
    }

    /** 释放资源 */
    public void destroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        initialized = false;
        currentCallback = null;
    }
}
