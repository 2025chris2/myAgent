package com.tzl.llongagent.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 仅为前端 SPA 路由提供 fallback，避免拦截 /assets/**、/api/** 等路径
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/chat").setViewName("forward:/index.html");
    }
}
