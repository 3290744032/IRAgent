package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MasteryRadarData {
    @SerializedName("labels")
    private List<String> labels;
    @SerializedName("values")
    private List<Double> values;

    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public List<Double> getValues() { return values; }
    public void setValues(List<Double> values) { this.values = values; }
}
