package com.suiyuan.iragent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plot3DConfig {

    private List<Vector> vectors;

    private List<Point3D> points;

    private List<String> planes;

    private List<Line3D> lines;

    private List<Box3D> boxes;

    private List<Sphere3D> spheres;

    private List<Cylinder3D> cylinders;

    private List<RevolutionSolid> solids;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Vector {
        private String name;
        private Double x;
        private Double y;
        private Double z;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point3D {
        private String name;
        private Double x;
        private Double y;
        private Double z;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line3D {
        private Double fromX;
        private Double fromY;
        private Double fromZ;
        private Double toX;
        private Double toY;
        private Double toZ;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Box3D {
        private String name;
        private Double cx;
        private Double cy;
        private Double cz;
        private Double width;
        private Double height;
        private Double depth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sphere3D {
        private String name;
        private Double cx;
        private Double cy;
        private Double cz;
        private Double radius;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cylinder3D {
        private String name;
        private Double cx;
        private Double cy;
        private Double cz;
        private Double radius;
        private Double height;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevolutionSolid {
        private String type;
        private String axis;
        private String expr;
        private Double xMin;
        private Double xMax;
        private Integer stepsX;
        private Integer stepsTheta;
        private Double opacity;
        private String color;
        private Boolean caps;
        private Boolean showGuides;
        private String guideColor;
        private Integer guideSteps;
    }
}
