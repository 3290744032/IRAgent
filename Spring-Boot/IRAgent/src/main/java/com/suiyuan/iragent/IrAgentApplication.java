package com.suiyuan.iragent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.suiyuan.iragent.mapper")
public class IrAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(IrAgentApplication.class, args);
    }

}
