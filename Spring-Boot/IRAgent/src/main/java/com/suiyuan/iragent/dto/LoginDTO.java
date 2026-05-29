package com.suiyuan.iragent.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录数据传输对象
 * 包含账号、密码、验证码等登录信息
 */
@Data
public class LoginDTO {
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String verifiCode;

    @NotBlank(message = "验证码UUID不能为空")
    private String uuid;
}