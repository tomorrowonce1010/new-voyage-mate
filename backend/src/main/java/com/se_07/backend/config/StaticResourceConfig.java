package com.se_07.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @PostConstruct
    public void init() {
        System.out.println("✅ StaticResourceConfig 已加载");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = "file:/root/voyagemate/new-voyage-mate/uploads/";

        // 不带 /api 前缀
        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPath + "images/");

        registry.addResourceHandler("/covers/**")
                .addResourceLocations(uploadPath + "covers/");

        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(uploadPath + "avatars/");
    }
}
