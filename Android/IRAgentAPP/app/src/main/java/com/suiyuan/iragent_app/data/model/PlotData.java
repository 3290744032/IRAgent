package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class PlotData {
    @SerializedName("type")
    private String type;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("function")
    private String function;
    
    @SerializedName("xRange")
    private Range xRange;
    
    @SerializedName("yRange")
    private Range yRange;
    
    @SerializedName("points")
    private List<List<Double>> points;
    
    @SerializedName("keyPoints")
    private List<KeyPoint> keyPoints;
    
    @SerializedName("gridLines")
    private Boolean gridLines;
    
    @SerializedName("axisLabels")
    private Map<String, String> axisLabels;
    
    @SerializedName("shapeParams")
    private Map<String, Object> shapeParams;
    
    @SerializedName("labels")
    private List<String> labels;
    
    @SerializedName("values")
    private List<Double> values;
    
    @SerializedName("chartType")
    private String chartType;
    
    @SerializedName("headers")
    private List<String> headers;
    
    @SerializedName("rows")
    private List<List<String>> rows;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getFunction() { return function; }
    public void setFunction(String function) { this.function = function; }
    
    public Range getxRange() { return xRange; }
    public void setxRange(Range xRange) { this.xRange = xRange; }
    
    public Range getyRange() { return yRange; }
    public void setyRange(Range yRange) { this.yRange = yRange; }
    
    public List<List<Double>> getPoints() { return points; }
    public void setPoints(List<List<Double>> points) { this.points = points; }
    
    public List<KeyPoint> getKeyPoints() { return keyPoints; }
    public void setKeyPoints(List<KeyPoint> keyPoints) { this.keyPoints = keyPoints; }
    
    public Boolean getGridLines() { return gridLines; }
    public void setGridLines(Boolean gridLines) { this.gridLines = gridLines; }
    
    public Map<String, String> getAxisLabels() { return axisLabels; }
    public void setAxisLabels(Map<String, String> axisLabels) { this.axisLabels = axisLabels; }
    
    public Map<String, Object> getShapeParams() { return shapeParams; }
    public void setShapeParams(Map<String, Object> shapeParams) { this.shapeParams = shapeParams; }
    
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    
    public List<Double> getValues() { return values; }
    public void setValues(List<Double> values) { this.values = values; }
    
    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    
    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }
    
    public List<List<String>> getRows() { return rows; }
    public void setRows(List<List<String>> rows) { this.rows = rows; }
    
    public boolean isFunction() { return "function".equals(type); }
    public boolean isData() { return "data".equals(type); }
    public boolean isGeometry() { return "geometry".equals(type); }
    public boolean isPoints() { return "points".equals(type); }
    public boolean isTable() { return "table".equals(type); }
}