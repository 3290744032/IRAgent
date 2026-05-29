package com.suiyuan.iragent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseSegment {

    private String type;
    private String content;
    @Deprecated
    private String expression;
    private PlotConfig plotConfig;
    private Plot3DConfig plot3dConfig;

    public static ResponseSegment text(String content) {
        return ResponseSegment.builder()
                .type("text")
                .content(content)
                .build();
    }

    public static ResponseSegment plot(PlotConfig plotConfig) {
        return ResponseSegment.builder()
                .type("plot")
                .plotConfig(plotConfig)
                .build();
    }

    public static ResponseSegment plot3d(Plot3DConfig plot3DConfig) {
        return ResponseSegment.builder()
                .type("plot3d")
                .plot3dConfig(plot3DConfig)
                .build();
    }

    public boolean isText() {
        return "text".equals(type);
    }

    public boolean isPlot() {
        return "plot".equals(type);
    }

    public boolean isPlot3d() {
        return "plot3d".equals(type);
    }

    public String getTextContent() {
        return isText() ? content : null;
    }

    public PlotConfig getPlotConfiguration() {
        return isPlot() ? plotConfig : null;
    }

    public Plot3DConfig getPlot3DConfiguration() {
        return isPlot3d() ? plot3dConfig : null;
    }
}
