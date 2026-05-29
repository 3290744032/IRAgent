package com.suiyuan.iragent.service;

import com.suiyuan.iragent.dto.LoginDTO;
import com.suiyuan.iragent.dto.RegisterDTO;
import com.suiyuan.iragent.utils.Result;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;
import java.util.Map;

/**
 * 用户服务接口
 * 提供用户注册、登录、信息管理等功能
 */
public interface UserService {
    /**
     * 用户登录
     * @param loginDTO 登录数据
     * @param bindingResult 数据验证结果
     * @return 登录结果
     */
    Result login(LoginDTO loginDTO, BindingResult bindingResult);

    /**
     * 用户注册
     * @param registerDTO 注册数据
     * @param bindingResult 数据验证结果
     * @return 注册结果
     */
    Result register(RegisterDTO registerDTO, BindingResult bindingResult);

    /**
     * 获取用户信息
     * @return 用户信息
     */
    Result getUserInfo();

    /**
     * 记录用户登录
     * @param userId 用户ID
     * @param token 登录令牌
     */
    void recordLogin(Long userId, String token);

    /**
     * 获取最近登录次数
     * @param userId 用户ID
     * @return 最近登录次数
     */
    Map<LocalDate, Integer> getRecentLoginCounts(Long userId);

    /**
     * 获取指定日期的登录次数
     * @param userId 用户ID
     * @param date 日期
     * @return 登录次数
     */
    int getLoginCountByDate(Long userId, LocalDate date);

    /**
     * 获取今日登录次数
     * @param userId 用户ID
     * @return 今日登录次数
     */
    int getTodayLoginCount(Long userId);

    /**
     * 清理过期的登录记录
     * @param userId 用户ID
     */
    void cleanExpiredLoginRecords(Long userId);
}