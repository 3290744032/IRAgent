package com.suiyuan.iragent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewBox {

    @Builder.Default
    private double xRange = 6;

    @Builder.Default
    private double yRange = 6;

    @Builder.Default
    private int width = 800;

    @Builder.Default
    private int height = 600;
}
