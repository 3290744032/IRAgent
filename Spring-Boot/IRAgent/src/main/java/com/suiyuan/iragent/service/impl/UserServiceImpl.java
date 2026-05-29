package com.suiyuan.iragent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.suiyuan.iragent.dto.LoginDTO;
import com.suiyuan.iragent.dto.RegisterDTO;
import com.suiyuan.iragent.entity.User;
import com.suiyuan.iragent.mapper.UserMapper;
import com.suiyuan.iragent.service.UserService;
import com.suiyuan.iragent.utils.RedisConstants;
import com.suiyuan.iragent.utils.Result;
import com.suiyuan.iragent.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserMapper userMapper;

    // 登录统计相关常量
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Result login(LoginDTO loginDTO, BindingResult bindingResult) {
        // 处理 DTO 校验结果
        if (bindingResult.hasErrors()) {
            return Result.fail(Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
        }

        // 检查验证码与 Redis 中的值是否匹配
        String redisKey = RedisConstants.AUTH_TOKEN_KEY + loginDTO.getUuid();
        Result fail = checkVerifiCode(redisKey, loginDTO.getVerifiCode());
        if (fail != null) return fail;

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPassword, loginDTO.getPassword());
        String account = loginDTO.getAccount();
        // 判断 account 是账号、邮箱还是手机号
        if (isPhone(account)) {
            queryWrapper.eq(User::getTelphone, account);
        } else if (isEmail(account)) {
            queryWrapper.eq(User::getEmail, account);
        } else {
            queryWrapper.eq(User::getAccount, account);
        }

        // 判断账号密码是否正确
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            return Result.fail("账号或密码错误！");
        }

        Long userId = user.getUserId();
        String token = UUID.fastUUID().toString();

        // 存储用户信息到Redis
        String userTokenKey = RedisConstants.USER_TOKEN_KEY + token;
        stringRedisTemplate.opsForValue().set(userTokenKey, userId.toString(), RedisConstants.USER_TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        // 记录登录信息
        recordLogin(userId, token);

        return Result.ok((Object) token, userId);
    }

    public Result checkVerifiCode(String redisKey, String verifiCode) {
        try {
            String storedValue = stringRedisTemplate.opsForValue().get(redisKey);
            if (StrUtil.isBlank(storedValue)) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("验证码已过期或不存在！");
            }
            if (!storedValue.equalsIgnoreCase(verifiCode)) {
                stringRedisTemplate.delete(redisKey);
                return Result.fail("验证码错误，请刷新重试！");
            }
            stringRedisTemplate.delete(redisKey);
        } catch (Exception e) {
            return Result.fail("系统错误，请稍后重试！");
        }
        return null;
    }

    private boolean isPhone(String str) {
        if (StrUtil.isBlank(str)) {
            return false;
        }
        return str.matches("^1[3-9]\\d{9}$");
    }

    private boolean isEmail(String str) {
        if (StrUtil.isBlank(str)) {
            return false;
        }
        return str.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    @Override
    public Result register(RegisterDTO registerDTO, BindingResult bindingResult) {
        // 处理 DTO 校验结果
        if (bindingResult.hasErrors()) {
            return Result.fail(Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
        }

        // 检查验证码与 Redis 中的值是否匹配
        String redisKey = RedisConstants.AUTH_TOKEN_KEY + registerDTO.getUuid();
        Result fail = checkVerifiCode(redisKey, registerDTO.getVerifiCode());
        if (fail != null) return fail;

        User user = BeanUtil.copyProperties(registerDTO, User.class);
        try {
            // 检查账号是否已存在
            User existingAccount = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getAccount, registerDTO.getAccount()));
            if (existingAccount != null) {
                return Result.fail("账号已存在，请使用其他账号！");
            }

            // 检查邮箱是否已存在
            User existingEmail = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, registerDTO.getEmail()));
            if (existingEmail != null) {
                return Result.fail("邮箱已存在，请使用其他邮箱！");
            }

            // 检查手机号是否已存在
            User existingTelphone = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getTelphone, registerDTO.getTelphone()));
            if (existingTelphone != null) {
                return Result.fail("手机号已存在，请使用其他手机号！");
            }

            // 插入用户
            int insertResult = userMapper.insert(user);
            if (insertResult != 1) {
                return Result.fail("注册失败！请稍后再试");
            }
            return Result.ok("注册成功！");
        } catch (Exception e) {
            return Result.fail("注册失败！请稍后再试");
        }
    }

    @Override
    public Result getUserInfo() {
        // 从UserHolder获取当前用户信息
        User currentUser = UserHolder.getUser();
        if (currentUser == null) {
            return Result.fail("用户未登录");
        }
        return Result.ok(currentUser);
    }

    @Override
    public void recordLogin(Long userId, String token) {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DATE_FORMATTER);

        // 构建Redis键
        String statsKey = RedisConstants.LOGIN_STATS_KEY_PREFIX + userId + ":" + dateStr;
        String tokensKey = RedisConstants.LOGIN_TOKENS_KEY_PREFIX + userId + ":" + dateStr;

        try {
            // 检查今日是否已经记录过此token的登录
            Boolean tokenExists = stringRedisTemplate.opsForSet().isMember(tokensKey, token);

            if (!Boolean.TRUE.equals(tokenExists)) {
                // 记录token，避免同一token重复计数
                stringRedisTemplate.opsForSet().add(tokensKey, token);

                // 增加登录次数
                stringRedisTemplate.opsForValue().increment(statsKey);

                // 设置过期时间（保留5天）
                stringRedisTemplate.expire(statsKey, RedisConstants.LOGIN_STATS_RETENTION_DAYS, TimeUnit.DAYS);
                stringRedisTemplate.expire(tokensKey, RedisConstants.LOGIN_STATS_RETENTION_DAYS, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.error("记录登录统计失败: userId={}", userId, e);
        }
    }

    @Override
    public Map<LocalDate, Integer> getRecentLoginCounts(Long userId) {
        Map<LocalDate, Integer> result = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 获取近5日的登录次数
        for (int i = 0; i < RedisConstants.LOGIN_STATS_RETENTION_DAYS; i++) {
            LocalDate date = today.minusDays(i);
            int count = getLoginCountByDate(userId, date);
            result.put(date, count);
        }

        return result;
    }

    @Override
    public int getLoginCountByDate(Long userId, LocalDate date) {
        String dateStr = date.format(DATE_FORMATTER);
        String statsKey = RedisConstants.LOGIN_STATS_KEY_PREFIX + userId + ":" + dateStr;

        try {
            String countStr = stringRedisTemplate.opsForValue().get(statsKey);
            return countStr != null ? Integer.parseInt(countStr) : 0;
        } catch (Exception e) {
            log.error("获取登录次数失败: userId={}, date={}", userId, dateStr, e);
            return 0;
        }
    }

    @Override
    public int getTodayLoginCount(Long userId) {
        return getLoginCountByDate(userId, LocalDate.now());
    }

    @Override
    public void cleanExpiredLoginRecords(Long userId) {
        try {
            // 获取所有相关的键
            String pattern = RedisConstants.LOGIN_STATS_KEY_PREFIX + userId + ":*";
            Set<String> statsKeys = stringRedisTemplate.keys(pattern);

            String tokenPattern = RedisConstants.LOGIN_TOKENS_KEY_PREFIX + userId + ":*";
            Set<String> tokenKeys = stringRedisTemplate.keys(tokenPattern);

            LocalDate cutoffDate = LocalDate.now().minusDays(RedisConstants.LOGIN_STATS_RETENTION_DAYS);

            // 清理过期的统计数据
            if (statsKeys != null) {
                for (String key : statsKeys) {
                    String dateStr = key.substring(key.lastIndexOf(":") + 1);
                    try {
                        LocalDate keyDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                        if (keyDate.isBefore(cutoffDate)) {
                            stringRedisTemplate.delete(key);
                        }
                    } catch (Exception e) {
                        // 如果日期解析失败，删除该键
                        stringRedisTemplate.delete(key);
                    }
                }
            }

            // 清理过期的token记录
            if (tokenKeys != null) {
                for (String key : tokenKeys) {
                    String dateStr = key.substring(key.lastIndexOf(":") + 1);
                    try {
                        LocalDate keyDate = LocalDate.parse(dateStr, DATE_FORMATTER);
                        if (keyDate.isBefore(cutoffDate)) {
                            stringRedisTemplate.delete(key);
                        }
                    } catch (Exception e) {
                        // 如果日期解析失败，删除该键
                        stringRedisTemplate.delete(key);
                    }
                }
            }

        } catch (Exception e) {
            log.error("清理过期登录记录失败: userId={}", userId, e);
        }
    }
}