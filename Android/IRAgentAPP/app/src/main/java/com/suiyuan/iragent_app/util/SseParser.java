package com.suiyuan.iragent_app.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SseParser {

    private static final String TAG = "SseParser";

    private final InputStream inputStream;
    private Callback callback;
    private V3Callback v3Callback;
    private volatile boolean isRunning = false;

    public SseParser(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setV3Callback(V3Callback v3Callback) {
        this.v3Callback = v3Callback;
    }

    public void stop() {
        isRunning = false;
    }

    public void parse() {
        isRunning = true;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            StringBuilder dataBuilder = new StringBuilder();

            while (isRunning && (line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (dataBuilder.length() > 0) {
                        String data = dataBuilder.toString().trim();
                        if (!data.isEmpty()) {
                            parseData(data);
                        }
                        dataBuilder.setLength(0);
                    }
                } else if (line.startsWith("data:")) {
                    String dataPart = line.substring(5).trim();
                    dataBuilder.append(dataPart);
                } else if (line.startsWith("event:")) {
                    // event字段可选，暂时忽略
                } else if (line.startsWith("id:")) {
                    // id字段可选，暂时忽略
                } else {
                    // 其他非标准行，可能是续行，追加到data
                    dataBuilder.append(line);
                }
            }

            // 处理最后一条消息（如果没有以空行结尾）
            if (dataBuilder.length() > 0) {
                parseData(dataBuilder.toString().trim());
            }
        } catch (IOException e) {
            if (callback != null && isRunning) {
                callback.onError("流读取错误: " + e.getMessage());
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // 忽略关闭异常
                }
            }
            isRunning = false;
        }
    }

    private void parseData(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return;
        }

        try {
            // 处理双重转义的情况：数据本身是带引号的JSON字符串
            String cleanJson = jsonStr.trim();
            if (cleanJson.startsWith("\"") && cleanJson.endsWith("\"")) {
                // 去掉首尾引号并处理转义
                cleanJson = cleanJson.substring(1, cleanJson.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
            
            JSONObject json = new JSONObject(cleanJson);
            String type = json.optString("type");

            Log.d(TAG, "Received SSE message: type=" + type + ", data=" + jsonStr);

            // 所有V3事件的payload都在data字段内
            org.json.JSONObject payload = json.optJSONObject("data");

            // V3 event: chunk (逐字流式文本)
            if ("chunk".equals(type) && v3Callback != null) {
                if (payload != null) {
                    String content = payload.optString("content", "");
                    if (!content.isEmpty()) {
                        v3Callback.onChunk(content);
                    }
                }
                return;
            }
            // V3 event: note_refs (笔记引用)
            if ("note_refs".equals(type) && v3Callback != null) {
                if (payload != null) {
                    org.json.JSONArray refs = payload.optJSONArray("noteRefs");
                    if (refs != null) {
                        java.util.List<com.suiyuan.iragent_app.data.model.v3.NoteRef> noteRefs = new java.util.ArrayList<>();
                        for (int i = 0; i < refs.length(); i++) {
                            org.json.JSONObject ref = refs.getJSONObject(i);
                            com.suiyuan.iragent_app.data.model.v3.NoteRef nr = new com.suiyuan.iragent_app.data.model.v3.NoteRef();
                            nr.setNoteFragment(ref.optString("noteFragment", ""));
                            nr.setSimilarity(ref.optDouble("similarity", 0));
                            noteRefs.add(nr);
                        }
                        v3Callback.onNoteRefs(noteRefs);
                    }
                }
                return;
            }
            // V3 event: step (批改进度)
            if ("step".equals(type) && v3Callback != null) {
                if (payload != null) {
                    String step = payload.optString("step", "");
                    String text = payload.optString("text", "");
                    int current = payload.optInt("current", 0);
                    int total = payload.optInt("total", 0);
                    v3Callback.onStep(step, text, current, total);
                }
                return;
            }
            // V3 event: complete (批改报告)
            if ("complete".equals(type) && v3Callback != null) {
                if (payload != null) {
                    String reportJson = payload.toString();
                    com.suiyuan.iragent_app.data.model.v3.GradingReport report =
                        new com.google.gson.Gson().fromJson(reportJson,
                            com.suiyuan.iragent_app.data.model.v3.GradingReport.class);
                    if (report != null) {
                        v3Callback.onComplete(report);
                    }
                }
                return;
            }
            // V3 event: done
            if ("done".equals(type) && v3Callback != null) {
                v3Callback.onDone();
                return;
            }

            if ("error".equals(type)) {
                int code = json.optInt("code", 500);
                String message = json.optString("message", "未知错误");
                if (callback != null) {
                    callback.onError("错误[" + code + "]: " + message);
                }
                if (v3Callback != null) {
                    v3Callback.onError("错误[" + code + "]: " + message);
                }
            } else if ("text".equals(type)) {
                String content = json.optString("content", "");
                if (callback != null && !content.isEmpty()) {
                    callback.onMessage(type, content, null);
                }
            } else if ("geogebra".equals(type) || "plot".equals(type)) {
                String expression = json.optString("expression", "");
                if (callback != null && !expression.isEmpty()) {
                    callback.onMessage(type, null, expression);
                }
            } else if ("plot3d".equals(type)) {
                String config = json.optString("config", "");
                if (callback != null && !config.isEmpty()) {
                    callback.onMessage(type, config, null);
                }
            } else if ("timeline".equals(type)) {
                String timelineJson = json.optString("timeline", "");
                if (callback != null && !timelineJson.isEmpty()) {
                    callback.onMessage(type, timelineJson, null);
                }
            } else if ("start".equals(type) || "done".equals(type)) {
                String message = json.optString("message", "");
                if (callback != null) {
                    callback.onMessage(type, message, null);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON解析错误: " + e.getMessage() + ", data=" + jsonStr);
            if (callback != null) {
                callback.onError("数据解析错误: " + e.getMessage());
            }
        }
    }

    /**
     * 从 ResponseObject 的 toString() 格式中提取真正的文本内容
     */
    private String extractRealContent(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }

        // 强制匹配 text='...' 之间的内容
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("text='(.*?)'(?:,|$|\\})", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            String content = matcher.group(1);
            // 处理转义字符
            return content.replace("\\n", "\n").replace("\\'", "'");
        }

        // 如果匹配不到，说明已经是纯文本了，直接返回
        return raw;
    }

    public interface Callback {
        void onMessage(String type, String content, String expression);
        void onError(String error);
    }

    public interface V3Callback {
        void onChunk(String content);
        void onNoteRefs(java.util.List<com.suiyuan.iragent_app.data.model.v3.NoteRef> noteRefs);
        void onStep(String step, String text, int current, int total);
        void onComplete(com.suiyuan.iragent_app.data.model.v3.GradingReport report);
        void onDone();
        void onError(String error);
    }
}
