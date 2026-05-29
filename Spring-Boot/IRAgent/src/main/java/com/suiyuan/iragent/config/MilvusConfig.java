package com.suiyuan.iragent.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Milvus 2.4 向量数据库配置
 */
@Configuration
@ConfigurationProperties(prefix = "milvus")
@ConditionalOnProperty(prefix = "milvus", name = "host")
@Data
public class MilvusConfig {

    private String host = "localhost";
    private int port = 19530;
    private String database = "default";

    @Bean
    public MilvusServiceClient milvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(database)
                .withConnectTimeout(10, TimeUnit.SECONDS)
                .withKeepAliveTime(60, TimeUnit.SECONDS)
                .withIdleTimeout(300, TimeUnit.SECONDS)
                .build();
        return new MilvusServiceClient(connectParam);
    }
}
