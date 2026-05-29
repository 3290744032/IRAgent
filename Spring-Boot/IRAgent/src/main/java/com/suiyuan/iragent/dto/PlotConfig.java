package com.suiyuan.iragent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 绘图配置DTO
 * 用于存储函数图像的绘制配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlotConfig {

    /**
     * 函数表达式，如 y = sin(x)
     */
    private String expr;

    /**
     * X轴最小值
     */
    private Double xMin;

    /**
     * X轴最大值
     */
    private Double xMax;

    /**
     * Y轴最小值
     */
    private Double yMin;

    /**
     * Y轴最大值
     */
    private Double yMax;

    /**
     * 渐近线列表（函数图像无限接近但永不相交的直线）
     * 格式：x=数值（垂直渐近线）或 y=数值（水平渐近线）
     * 示例：["x=-1.5708", "x=1.5708"]（tan函数的垂直渐近线）
     *       ["y=1.5708", "y=-1.5708"]（arctan函数的水平渐近线）
     */
    private List<String> asymptotes;

    /**
     * 值域边界线列表（函数值域的上下限，不是渐近线）
     * 格式：y=数值
     * 示例：["y=1", "y=-1"]（sin/cos函数的值域边界）
     *       ["y=0"]（x^2函数的最小值边界）
     */
    private List<String> bounds;

    /**
     * 关键点列表
     */
    private List<Point> points;

    /**
     * 点坐标内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        /**
         * 点标签，必须使用大写字母，如 A, B, C, O（原点）
         */
        private String label;

        /**
         * X坐标
         */
        private Double x;

        /**
         * Y坐标
         */
        private Double y;

        /**
         * 是否显示标签
         */
        @Builder.Default
        private Boolean showLabel = true;

        public Point(String label, Double x, Double y) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.showLabel = true;
        }
    }

    /**
     * 获取默认的arctan配置
     * arctan(x) 有水平渐近线 y=±π/2，无边界
     */
    public static PlotConfig getDefaultArctanConfig() {
        return PlotConfig.builder()
                .expr("y = arctan(x)")
                .xMin(-5.2)
                .xMax(5.2)
                .yMin(-2.05)
                .yMax(2.05)
                .asymptotes(List.of("y=1.5708", "y=-1.5708"))
                .bounds(null)
                .points(List.of(
                        new Point("O", 0.0, 0.0),
                        new Point("A", 1.0, 0.7854),
                        new Point("B", -1.0, -0.7854)
                ))
                .build();
    }

    /**
     * 获取默认的tan配置
     * tan(x) 有垂直渐近线 x=±π/2，无边界
     */
    public static PlotConfig getDefaultTanConfig() {
        return PlotConfig.builder()
                .expr("y = tan(x)")
                .xMin(-4.7)
                .xMax(4.7)
                .yMin(-5.0)
                .yMax(5.0)
                .asymptotes(List.of("x=-1.5708", "x=1.5708"))
                .bounds(null)
                .points(List.of(
                        new Point("O", 0.0, 0.0),
                        new Point("A", 0.7854, 1.0),
                        new Point("B", -0.7854, -1.0)
                ))
                .build();
    }

    /**
     * 获取默认的sin配置
     * sin(x) 有值域边界 y=±1，无渐近线
     */
    public static PlotConfig getDefaultSinConfig() {
        return PlotConfig.builder()
                .expr("y = sin(x)")
                .xMin(-6.5)
                .xMax(6.5)
                .yMin(-1.2)
                .yMax(1.2)
                .asymptotes(null)
                .bounds(List.of("y=1", "y=-1"))
                .points(List.of(
                        new Point("O", 0.0, 0.0),
                        new Point("A", 1.5708, 1.0),
                        new Point("B", -1.5708, -1.0)
                ))
                .build();
    }

    /**
     * 获取默认的cos配置
     * cos(x) 有值域边界 y=±1，无渐近线
     */
    public static PlotConfig getDefaultCosConfig() {
        return PlotConfig.builder()
                .expr("y = cos(x)")
                .xMin(-6.5)
                .xMax(6.5)
                .yMin(-1.2)
                .yMax(1.2)
                .asymptotes(null)
                .bounds(List.of("y=1", "y=-1"))
                .points(List.of(
                        new Point("A", 0.0, 1.0),
                        new Point("B", 1.5708, 0.0),
                        new Point("C", -1.5708, 0.0)
                ))
                .build();
    }

    /**
     * 获取默认的x^2配置
     * x^2 有值域下边界 y=0，无渐近线
     */
    public static PlotConfig getDefaultX2Config() {
        return PlotConfig.builder()
                .expr("y = x^2")
                .xMin(-3.0)
                .xMax(3.0)
                .yMin(-0.5)
                .yMax(9.0)
                .asymptotes(null)
                .bounds(List.of("y=0"))
                .points(List.of(
                        new Point("O", 0.0, 0.0),
                        new Point("A", 1.0, 1.0),
                        new Point("B", -1.0, 1.0)
                ))
                .build();
    }

    /**
     * 根据函数表达式获取默认配置
     */
    public static PlotConfig getDefaultConfig(String expr) {
        if (expr == null) return new PlotConfig();

        String lowerExpr = expr.toLowerCase();

        if (lowerExpr.contains("arctan")) {
            return getDefaultArctanConfig();
        } else if (lowerExpr.contains("tan") && !lowerExpr.contains("arctan")) {
            return getDefaultTanConfig();
        } else if (lowerExpr.contains("sin")) {
            return getDefaultSinConfig();
        } else if (lowerExpr.contains("cos")) {
            return getDefaultCosConfig();
        } else if (lowerExpr.contains("^2")) {
            return getDefaultX2Config();
        }

        return PlotConfig.builder()
                .expr(expr)
                .xMin(-10.0)
                .xMax(10.0)
                .yMin(-10.0)
                .yMax(10.0)
                .build();
    }

    /**
     * 判断是否为周期函数
     */
    public static boolean isPeriodicFunction(String expr) {
        if (expr == null) return false;
        String lower = expr.toLowerCase();
        return lower.contains("sin") || lower.contains("cos") || lower.contains("tan");
    }

    /**
     * 判断是否为奇函数
     */
    public static boolean isOddFunction(String expr) {
        if (expr == null) return false;
        String lower = expr.toLowerCase();
        return lower.contains("arctan") || lower.contains("tan") || lower.contains("^3");
    }

    /**
     * 判断是否为偶函数
     */
    public static boolean isEvenFunction(String expr) {
        if (expr == null) return false;
        String lower = expr.toLowerCase();
        return lower.contains("cos") || lower.contains("^2") || lower.contains("abs");
    }
}
