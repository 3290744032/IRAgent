package com.suiyuan.iragent.controller;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.suiyuan.iragent.dto.LoginDTO;
import com.suiyuan.iragent.dto.RegisterDTO;
import com.suiyuan.iragent.service.UserService;
import com.suiyuan.iragent.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.suiyuan.iragent.utils.RedisConstants.AUTH_TOKEN_KEY;
import static com.suiyuan.iragent.utils.RedisConstants.LOGIN_CODE_TTL_SECONDS;

/**
 * 用户认证控制器
 * 处理用户登录、注册、验证码生成等认证相关操作
 */
@Tag(name = "用户认证", description = "用户登录、注册、验证码等操作")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthController(UserService userService, StringRedisTemplate stringRedisTemplate) {
        this.userService = userService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 用户登录接口
     */
    @Operation(summary = "登录", description = "使用账号、密码、验证码进行登录操作")
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginDTO, BindingResult bindingResult) {
        // 处理参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError() != null ? 
                    bindingResult.getFieldError().getDefaultMessage() : "参数校验失败";
            return ApiResponse.badRequest(errorMsg);
        }

        // 调用用户服务登录
        var result = userService.login(loginDTO, bindingResult);

        if (result.isSuccess()) {
            Map<String, Object> data = new HashMap<>();
            data.put("token", result.getData());
            data.put("userId", result.getUserId());
            data.put("role", "admin".equals(loginDTO.getAccount()) ? "admin" : "user");
            return ApiResponse.success("登录成功", data);
        } else {
            return ApiResponse.error(result.getMessage());
        }
    }

    /**
     * 用户注册接口
     */
    @Operation(summary = "注册", description = "使用账号关键信息、验证码进行注册操作")
    @PostMapping("/register")
    public ApiResponse<String> register(@Valid @RequestBody RegisterDTO registerDTO, BindingResult bindingResult) {
        // 处理参数校验
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getFieldError() != null ? 
                    bindingResult.getFieldError().getDefaultMessage() : "参数校验失败";
            return ApiResponse.badRequest(errorMsg);
        }

        // 调用用户服务注册
        var result = userService.register(registerDTO, bindingResult);

        if (result.isSuccess()) {
            return ApiResponse.success("注册成功");
        } else {
            return ApiResponse.error(result.getMessage());
        }
    }

    /**
     * 获取图片验证码接口
     */
    @Operation(summary = "获取验证码", description = "获取图片验证码，验证码内容存入 Redis")
    @GetMapping("/getVerifiCodeImage")
    public void getVerifiCodeImage(HttpServletResponse response) throws IOException {
        // 使用Hutool的CaptchaUtil生成验证码
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String verifiCode = captcha.getCode();
        
        if (StrUtil.isBlank(verifiCode)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":400,\"message\":\"验证码生成失败！\"}");
            return;
        }
        
        String uuid = UUID.randomUUID().toString();
        String redisKey = AUTH_TOKEN_KEY + uuid;
        stringRedisTemplate.opsForValue().set(redisKey, verifiCode, LOGIN_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        
        String storedValue = stringRedisTemplate.opsForValue().get(redisKey);
        if (StrUtil.isBlank(storedValue)) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"code\":500,\"message\":\"验证码存储失败！\"}");
            return;
        }
        
        response.setContentType("image/jpeg");
        response.setHeader("X-Verification-UUID", uuid);
        captcha.write(response.getOutputStream());
    }
}
