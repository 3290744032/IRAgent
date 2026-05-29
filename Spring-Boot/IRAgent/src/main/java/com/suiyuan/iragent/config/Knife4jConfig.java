package com.suiyuan.iragent.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API接口文档，运行后网址为http://localhost:8080/api/doc.html
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "智研Agent API",
                description = "IRAgent 项目的 RESTful API 文档",
                version = "1.0",
                contact = @Contact(
                        name = "支持团队",
                        email = "support@suiyuan.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080/api", description = "本地开发环境")
        }
)
public class Knife4jConfig {
    /**
     * 自定义OpenAPI配置
     * 配置全局的Token认证方案
     * 
     * @return OpenAPI配置对象
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("TokenAuth")) // 全局应用 token 认证
                .components(new Components()
                        .addSecuritySchemes("TokenAuth", new SecurityScheme()
                                .name("token")
                                .type(SecurityScheme.Type.APIKEY) // 使用 API Key 类型
                                .in(SecurityScheme.In.HEADER) // 指定为 Header 参数
                                .description("用户认证 Token，格式：Bearer <token>")
                        )
                );
    }

    /**
     * 配置API分组
     * 将所有API接口归为一组进行展示
     * 
     * @return 分组API配置对象
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("All APIs")
                .pathsToMatch("/**")
                .build();
    }

}
