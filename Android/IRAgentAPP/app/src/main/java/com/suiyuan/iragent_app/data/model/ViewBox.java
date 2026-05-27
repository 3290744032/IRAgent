package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class ViewBox {

    @SerializedName("xRange")
    private double xRange = 6.0;

    @SerializedName("yRange")
    private double yRange = 4.0;

    @SerializedName("xMin")
    private Double xMin;
    @SerializedName("xMax")
    private Double xMax;
    @SerializedName("yMin")
    private Double yMin;
    @SerializedName("yMax")
    private Double yMax;
    @SerializedName("pxPerUnit")
    private Double pxPerUnit;

    public double getXRange() {
        if (xRange == 6.0 && xMin != null && xMax != null) {
            return Math.max(Math.abs(xMin), Math.abs(xMax));
        }
        return xRange;
    }

    public double getYRange() {
        if (yRange == 4.0 && yMin != null && yMax != null) {
            return Math.max(Math.abs(yMin), Math.abs(yMax));
        }
        return yRange;
    }

    public Double getXMin() { return xMin; }
    public Double getXMax() { return xMax; }
    public Double getYMin() { return yMin; }
    public Double getYMax() { return yMax; }
    public Double getPxPerUnit() { return pxPerUnit; }
}
