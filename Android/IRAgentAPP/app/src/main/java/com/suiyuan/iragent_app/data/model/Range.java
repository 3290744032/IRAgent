package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class Range {
    @SerializedName("min")
    private Double min;
    
    @SerializedName("max")
    private Double max;

    public Range() {}

    public Range(Double min, Double max) {
        this.min = min;
        this.max = max;
    }

    public Double getMin() { return min; }
    public void setMin(Double min) { this.min = min; }
    
    public Double getMax() { return max; }
    public void setMax(Double max) { this.max = max; }
    
    public float getMinFloat() { return min != null ? min.floatValue() : 0f; }
    public float getMaxFloat() { return max != null ? max.floatValue() : 0f; }
}