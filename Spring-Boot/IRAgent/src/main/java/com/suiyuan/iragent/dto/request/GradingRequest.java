package com.suiyuan.iragent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "试卷批改请求")
public class GradingRequest {

    @NotBlank
    @Schema(description = "试卷文本内容（OCR 识别后或直接粘贴）",
            example = "1. 求 f(x)=x²+2x+1 的极值\n答：极值为 3\n2. 已知 sinα=3/5，求 cosα\n答：cosα=4/5")
    private String content;

    @Schema(description = "学科类型", example = "math")
    private String subjectType;

    @Schema(description = "总分", example = "100")
    private int maxScore = 100;
}
