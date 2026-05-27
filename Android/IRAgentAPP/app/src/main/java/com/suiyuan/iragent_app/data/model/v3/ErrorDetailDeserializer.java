package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ErrorDetailDeserializer implements JsonDeserializer<ErrorDetail> {
    @Override
    public ErrorDetail deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        Gson gson = new Gson();
        ErrorDetail detail = new ErrorDetail();

        if (obj.has("id") && !obj.get("id").isJsonNull())
            detail.setId(obj.get("id").getAsString());
        if (obj.has("question_text") && !obj.get("question_text").isJsonNull())
            detail.setQuestionText(obj.get("question_text").getAsString());
        if (obj.has("student_answer") && !obj.get("student_answer").isJsonNull())
            detail.setStudentAnswer(obj.get("student_answer").getAsString());
        if (obj.has("correct_answer") && !obj.get("correct_answer").isJsonNull())
            detail.setCorrectAnswer(obj.get("correct_answer").getAsString());
        if (obj.has("knowledge_point") && !obj.get("knowledge_point").isJsonNull())
            detail.setKnowledgePoint(obj.get("knowledge_point").getAsString());
        if (obj.has("error_type") && !obj.get("error_type").isJsonNull())
            detail.setErrorType(obj.get("error_type").getAsString());
        if (obj.has("subject") && !obj.get("subject").isJsonNull())
            detail.setSubject(obj.get("subject").getAsString());
        if (obj.has("mastered") && !obj.get("mastered").isJsonNull())
            detail.setMastered(obj.get("mastered").getAsBoolean());

        // diagnosis_json → JSONB: {"type":"jsonb","value":"{...}"}
        if (obj.has("diagnosis_json") && !obj.get("diagnosis_json").isJsonNull()) {
            JsonObject wrapper = obj.get("diagnosis_json").getAsJsonObject();
            if (wrapper.has("value") && !wrapper.get("value").isJsonNull()) {
                detail.setDiagnosis(parseDiagnosisJson(wrapper.get("value").getAsString()));
            }
        }

        // similar_questions → JSONB: {"type":"jsonb","value":"[...]"}
        if (obj.has("similar_questions") && !obj.get("similar_questions").isJsonNull()) {
            JsonObject wrapper = obj.get("similar_questions").getAsJsonObject();
            if (wrapper.has("value") && !wrapper.get("value").isJsonNull()) {
                try {
                    Type listType = new TypeToken<List<SimilarQuestion>>() {}.getType();
                    List<SimilarQuestion> sims = gson.fromJson(wrapper.get("value").getAsString(), listType);
                    detail.setSimilarQuestions(sims);
                } catch (JsonParseException ignored) { }
            }
        }

        return detail;
    }

    private DiagnosisJson parseDiagnosisJson(String jsonStr) {
        try {
            JsonObject data = new Gson().fromJson(jsonStr, JsonObject.class);
            DiagnosisJson diag = new DiagnosisJson();
            diag.setCalculationError(extractFromCodeBlock(data, "calculation_error", "mostLikelyError"));
            diag.setFormulaConfusion(extractFromCodeBlock(data, "formula_confusion", "analysis"));
            diag.setPrerequisiteCheck(extractFromCodeBlock(data, "prerequisite_check", "analysis"));
            return diag;
        } catch (JsonParseException e) {
            return null;
        }
    }

    private DiagnosisItem extractFromCodeBlock(JsonObject parent, String key, String analysisField) {
        if (!parent.has(key) || parent.get(key).isJsonNull()) return null;
        String raw = parent.get(key).getAsString();
        String jsonContent = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
        try {
            JsonObject data = new Gson().fromJson(jsonContent, JsonObject.class);
            DiagnosisItem item = new DiagnosisItem();
            if (data.has(analysisField) && !data.get(analysisField).isJsonNull()) {
                item.setAnalysis(data.get(analysisField).getAsString());
            }
            return item;
        } catch (JsonParseException e) {
            DiagnosisItem fallback = new DiagnosisItem();
            fallback.setAnalysis(raw);
            return fallback;
        }
    }
}
