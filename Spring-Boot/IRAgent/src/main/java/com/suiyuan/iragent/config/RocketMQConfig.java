package com.suiyuan.iragent.config;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 5.x 配置
 *
 * rocketmq-spring-boot-starter 自动配置 NameServer、Producer、Consumer，
 * 通过 application.yaml 中 rocketmq.* 配置即可。
 * 本类留空，作为后续自定义 Producer/Consumer Bean 的占位点。
 */
@Configuration
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
public class RocketMQConfig {
    // RocketMQTemplate 由 Starter 自动创建，直接 @Autowired 注入即可使用
}
