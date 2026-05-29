package com.suiyuan.iragent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "错题诊断请求")
public class DiagnosisRequest {

    @NotBlank
    @Schema(description = "题目内容", example = "已知函数 f(x)=x²+2x+1，求 f(x) 在区间 [-1, 1] 上的最大值与最小值。")
    private String question;

    @Schema(description = "学生的错误答案", example = "最大值为0，最小值为-2")
    private String studentAnswer;

    @Schema(description = "学科类型", example = "math")
    private String subjectType;
}
