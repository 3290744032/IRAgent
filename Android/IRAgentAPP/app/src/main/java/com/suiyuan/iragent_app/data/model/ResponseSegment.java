package com.suiyuan.iragent_app.data.model;

import com.google.gson.annotations.SerializedName;

public class ResponseSegment {
    @SerializedName("type")
    private String type;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("expression")
    private String expression;
    
    @SerializedName("data")
    private PlotData data;

    @SerializedName("plot3d_config")
    private String plot3dConfig;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    
    public PlotData getData() { return data; }
    public void setData(PlotData data) { this.data = data; }
    
    public String getPlot3dConfig() { return plot3dConfig; }
    public void setPlot3dConfig(String plot3dConfig) { this.plot3dConfig = plot3dConfig; }
    
    public boolean isText() { return "text".equals(type); }
    public boolean isGeogebra() { return "geogebra".equals(type) || "desmos".equals(type); }
    public boolean isPlot() { return "plot".equals(type); }
    public boolean isPlot3D() { return "plot3d".equals(type); }
}
