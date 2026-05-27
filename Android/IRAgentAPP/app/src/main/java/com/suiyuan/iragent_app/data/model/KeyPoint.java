package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class KeyPoint {
    @SerializedName("x")
    private Double x;
    
    @SerializedName("y")
    private Double y;
    
    @SerializedName("label")
    private String label;

    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }
    
    public Double getY() { return y; }
    public void setY(Double y) { this.y = y; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public float getXFloat() { return x != null ? x.floatValue() : 0f; }
    public float getYFloat() { return y != null ? y.floatValue() : 0f; }
}