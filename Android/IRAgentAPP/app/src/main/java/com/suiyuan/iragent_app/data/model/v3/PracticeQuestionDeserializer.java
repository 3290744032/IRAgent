package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PracticeQuestionDeserializer implements JsonDeserializer<PracticeQuestion> {
    @Override
    public PracticeQuestion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        Gson gson = new Gson();
        PracticeQuestion q = new PracticeQuestion();

        if (obj.has("id") && !obj.get("id").isJsonNull())
            q.setId(obj.get("id").getAsString());
        if (obj.has("questionText") && !obj.get("questionText").isJsonNull())
            q.setQuestionText(obj.get("questionText").getAsString());
        if (obj.has("questionType") && !obj.get("questionType").isJsonNull())
            q.setQuestionType(obj.get("questionType").getAsString());
        if (obj.has("difficulty") && !obj.get("difficulty").isJsonNull())
            q.setDifficulty(obj.get("difficulty").getAsInt());
        if (obj.has("knowledgePoint") && !obj.get("knowledgePoint").isJsonNull())
            q.setKnowledgePoint(obj.get("knowledgePoint").getAsString());
        if (obj.has("chapter") && !obj.get("chapter").isJsonNull())
            q.setChapter(obj.get("chapter").getAsString());
        if (obj.has("score") && !obj.get("score").isJsonNull())
            q.setScore(obj.get("score").getAsInt());
        if (obj.has("index") && !obj.get("index").isJsonNull())
            q.setIndex(obj.get("index").getAsInt());
        if (obj.has("source") && !obj.get("source").isJsonNull())
            q.setSource(obj.get("source").getAsString());

        // options: can be null, string array, or JSONB {"type":"jsonb","value":"[...]"}
        if (obj.has("options") && !obj.get("options").isJsonNull()) {
            JsonElement optEl = obj.get("options");
            if (optEl.isJsonArray()) {
                List<String> opts = new ArrayList<>();
                for (JsonElement e : optEl.getAsJsonArray()) {
                    opts.add(e.getAsString());
                }
                q.setOptions(opts);
            } else if (optEl.isJsonObject()) {
                JsonObject optObj = optEl.getAsJsonObject();
                if (optObj.has("value") && !optObj.get("value").isJsonNull()) {
                    try {
                        String valueStr = optObj.get("value").getAsString();
                        JsonArray arr = gson.fromJson(valueStr, JsonArray.class);
                        List<String> opts = new ArrayList<>();
                        for (JsonElement e : arr) {
                            opts.add(e.getAsString());
                        }
                        q.setOptions(opts);
                    } catch (JsonParseException ignored) { }
                }
            }
        }

        return q;
    }
}
