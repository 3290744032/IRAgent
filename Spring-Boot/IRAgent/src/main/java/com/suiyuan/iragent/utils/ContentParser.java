package com.suiyuan.iragent.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suiyuan.iragent.dto.Plot3DConfig;
import com.suiyuan.iragent.dto.PlotConfig;
import com.suiyuan.iragent.dto.ResponseSegment;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ContentParser {

    private static final Pattern PLOT_BLOCK_PATTERN = Pattern.compile(
            "【PLOT】\\s*([\\s\\S]*?)【END】",
            Pattern.MULTILINE
    );

    private static final Pattern PLOT3D_BLOCK_PATTERN = Pattern.compile(
            "【PLOT3D】\\s*([\\s\\S]*?)【END】",
            Pattern.MULTILINE
    );

    private static final Pattern GEOGEBRA_BLOCK_PATTERN = Pattern.compile(
            "【GEGEBRA】\\s*([\\s\\S]*?)【END】",
            Pattern.MULTILINE
    );

    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile(
            "^\\s*([a-zA-Z]+)\\s*:\\s*(.+)$",
            Pattern.MULTILINE
    );

    private static final Pattern POINT_PATTERN = Pattern.compile(
            "([A-Z])\\s*\\(\\s*([+-]?\\d+\\.?\\d*)\\s*,\\s*([+-]?\\d+\\.?\\d*)\\s*\\)"
    );

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<ResponseSegment> parse(String content) {
        List<ResponseSegment> segments = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return segments;
        }

        List<SegmentPosition> allSegments = new ArrayList<>();
        Matcher plotMatcher = PLOT_BLOCK_PATTERN.matcher(content);
        while (plotMatcher.find()) {
            allSegments.add(new SegmentPosition(plotMatcher.start(), plotMatcher.end(), "plot", plotMatcher.group(1)));
        }

        Matcher plot3dMatcher = PLOT3D_BLOCK_PATTERN.matcher(content);
        while (plot3dMatcher.find()) {
            allSegments.add(new SegmentPosition(plot3dMatcher.start(), plot3dMatcher.end(), "plot3d", plot3dMatcher.group(1)));
        }

        allSegments.sort((a, b) -> Integer.compare(a.start, b.start));

        int lastEnd = 0;
        for (SegmentPosition seg : allSegments) {
            if (seg.start > lastEnd) {
                String textBefore = content.substring(lastEnd, seg.start).trim();
                if (!textBefore.isEmpty()) {
                    segments.add(ResponseSegment.text(textBefore));
                }
            }

            if ("plot".equals(seg.type)) {
                PlotConfig plotConfig = parsePlotConfig(seg.content);
                segments.add(ResponseSegment.plot(plotConfig));
            } else if ("plot3d".equals(seg.type)) {
                Plot3DConfig plot3DConfig = parsePlot3DConfig(seg.content);
                segments.add(ResponseSegment.plot3d(plot3DConfig));
            }

            lastEnd = seg.end;
        }

        if (lastEnd < content.length()) {
            String textAfter = content.substring(lastEnd).trim();
            if (!textAfter.isEmpty()) {
                segments.add(ResponseSegment.text(textAfter));
            }
        }

        return segments;
    }

    private static PlotConfig parsePlotConfig(String plotBlock) {
        PlotConfig config = new PlotConfig();

        Matcher kvMatcher = KEY_VALUE_PATTERN.matcher(plotBlock);
        while (kvMatcher.find()) {
            String key = kvMatcher.group(1).trim().toLowerCase();
            String value = kvMatcher.group(2).trim();

            switch (key) {
                case "expr" -> config.setExpr(value);
                case "xmin" -> config.setXMin(parseDouble(value));
                case "xmax" -> config.setXMax(parseDouble(value));
                case "ymin" -> config.setYMin(parseDouble(value));
                case "ymax" -> config.setYMax(parseDouble(value));
                case "asymptotes" -> config.setAsymptotes(parseLines(value));
                case "bounds" -> config.setBounds(parseLines(value));
                case "points" -> config.setPoints(parsePoints(value));
                default -> log.warn("未知的PLOT配置项: {}", key);
            }
        }

        return config;
    }

    private static Plot3DConfig parsePlot3DConfig(String plot3dBlock) {
        try {
            String jsonContent = plot3dBlock.trim();
            return objectMapper.readValue(jsonContent, Plot3DConfig.class);
        } catch (Exception e) {
            log.error("解析PLOT3D配置失败: {}", e.getMessage());
            return new Plot3DConfig();
        }
    }

    private static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("无法解析数值: {}", value);
            return null;
        }
    }

    private static List<String> parseLines(String value) {
        List<String> lines = new ArrayList<>();
        String[] parts = value.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private static List<PlotConfig.Point> parsePoints(String value) {
        List<PlotConfig.Point> points = new ArrayList<>();

        Matcher matcher = POINT_PATTERN.matcher(value);
        while (matcher.find()) {
            String label = matcher.group(1);
            double x = Double.parseDouble(matcher.group(2));
            double y = Double.parseDouble(matcher.group(3));
            points.add(new PlotConfig.Point(label, x, y));
        }

        return points;
    }

    public static String cleanSolution(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        content = content.replaceAll("【PLOT】[\\s\\S]*?【END】", "").trim();
        content = content.replaceAll("【PLOT3D】[\\s\\S]*?【END】", "").trim();
        content = content.replaceAll("【GEGEBRA】[\\s\\S]*?【END】", "").trim();
        return content;
    }

    public static List<String> extractGeogebraExpressions(String content) {
        List<String> expressions = new ArrayList<>();
        if (content == null) {
            return expressions;
        }

        Matcher plotMatcher = PLOT_BLOCK_PATTERN.matcher(content);
        while (plotMatcher.find()) {
            PlotConfig config = parsePlotConfig(plotMatcher.group(1));
            if (config.getExpr() != null) {
                expressions.add(config.getExpr());
            }
        }

        if (expressions.isEmpty()) {
            Matcher blockMatcher = GEOGEBRA_BLOCK_PATTERN.matcher(content);
            while (blockMatcher.find()) {
                String geogebraBlock = blockMatcher.group(1);
                String[] lines = geogebraBlock.split("\\r?\\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("【")) {
                        expressions.add(line);
                    }
                }
            }
        }

        return expressions;
    }

    public static List<PlotConfig> extractPlotConfigs(String content) {
        List<PlotConfig> configs = new ArrayList<>();
        if (content == null) {
            return configs;
        }

        Matcher plotMatcher = PLOT_BLOCK_PATTERN.matcher(content);
        while (plotMatcher.find()) {
            configs.add(parsePlotConfig(plotMatcher.group(1)));
        }

        if (configs.isEmpty()) {
            Matcher geogebraMatcher = GEOGEBRA_BLOCK_PATTERN.matcher(content);
            while (geogebraMatcher.find()) {
                configs.add(convertGeogebraToPlot(geogebraMatcher.group(1)));
            }
        }

        return configs;
    }

    private static PlotConfig convertGeogebraToPlot(String geogebraBlock) {
        PlotConfig config = new PlotConfig();
        List<PlotConfig.Point> points = new ArrayList<>();
        List<String> asymptotes = new ArrayList<>();
        List<String> bounds = new ArrayList<>();

        String[] lines = geogebraBlock.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.matches("^y\\s*=\\s*.+")) {
                config.setExpr(line);
            } else if (line.matches("^y\\s*=\\s*[+-]?\\d+\\.?\\d*$")) {
                bounds.add(line);
            } else if (line.matches("^x\\s*=\\s*[+-]?\\d+\\.?\\d*$")) {
                asymptotes.add(line);
            } else if (line.matches("^[A-Za-z]\\s*=\\s*\\([\\d\\.\\s,]+\\)$")) {
                Matcher pointMatcher = Pattern.compile("([A-Za-z])\\s*=\\s*\\(\\s*([\\d\\.]+)\\s*,\\s*([\\d\\.]+)\\s*\\)").matcher(line);
                if (pointMatcher.find()) {
                    String label = pointMatcher.group(1).toUpperCase();
                    points.add(new PlotConfig.Point(
                            label,
                            Double.parseDouble(pointMatcher.group(2)),
                            Double.parseDouble(pointMatcher.group(3))
                    ));
                }
            }
        }

        config.setPoints(points);
        config.setAsymptotes(asymptotes.isEmpty() ? null : asymptotes);
        config.setBounds(bounds.isEmpty() ? null : bounds);

        setDefaultRange(config);

        return config;
    }

    private static void setDefaultRange(PlotConfig config) {
        String expr = config.getExpr();
        if (expr == null) return;

        String lowerExpr = expr.toLowerCase();

        if (lowerExpr.contains("arctan")) {
            config.setXMin(-5.2);
            config.setXMax(5.2);
            config.setYMin(-2.05);
            config.setYMax(2.05);
        } else if (lowerExpr.contains("tan") && !lowerExpr.contains("arctan")) {
            config.setXMin(-4.7);
            config.setXMax(4.7);
            config.setYMin(-5.0);
            config.setYMax(5.0);
        } else if (lowerExpr.contains("sin") || lowerExpr.contains("cos")) {
            config.setXMin(-6.5);
            config.setXMax(6.5);
            config.setYMin(-1.2);
            config.setYMax(1.2);
        } else if (lowerExpr.contains("^2")) {
            config.setXMin(-3.0);
            config.setXMax(3.0);
            config.setYMin(-0.5);
            config.setYMax(9.0);
        } else if (lowerExpr.contains("^3")) {
            config.setXMin(-3.0);
            config.setXMax(3.0);
            config.setYMin(-9.0);
            config.setYMax(9.0);
        }
    }

    private static class SegmentPosition {
        int start;
        int end;
        String type;
        String content;

        SegmentPosition(int start, int end, String type, String content) {
            this.start = start;
            this.end = end;
            this.type = type;
            this.content = content;
        }
    }
}
