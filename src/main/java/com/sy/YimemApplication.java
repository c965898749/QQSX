package com.sy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * YIMEM Spring Boot 启动类
 */
@SpringBootApplication
@MapperScan("com.sy.mapper")
@ServletComponentScan(basePackages = "com.sy")
@ComponentScan(basePackages = {"com.sy"})
@EnableScheduling
public class YimemApplication {
    public static void main(String[] args) {
        SpringApplication.run(YimemApplication.class, args);
    }
}