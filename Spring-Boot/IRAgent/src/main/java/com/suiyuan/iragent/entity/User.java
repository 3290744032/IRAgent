package com.suiyuan.iragent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 存储用户基本信息
 */
@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long userId;
    private String account;
    private String password;
    private String email;
    private String telphone;
    private String nickname;
    private Integer age;
    private String gender;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}