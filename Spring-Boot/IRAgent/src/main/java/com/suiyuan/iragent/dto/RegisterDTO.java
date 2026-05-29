package com.suiyuan.iragent.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 注册数据传输对象
 * 包含用户名、密码、邮箱、手机号等注册信息
 */
@Data
public class RegisterDTO {
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String telphone;

    @NotBlank(message = "验证码不能为空")
    private String verifiCode;

    @NotBlank(message = "验证码UUID不能为空")
    private String uuid;
}