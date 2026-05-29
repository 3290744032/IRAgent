package com.suiyuan.iragent.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AskRequest {
    @NotBlank(message = "问题不能为空")
    private String question;
}
