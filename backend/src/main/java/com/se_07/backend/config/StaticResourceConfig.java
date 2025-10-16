package com.se_07.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uploadPath = "file:/root/voyagemate/new-voyage-mate/uploads/";

        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPath + "images/");
        registry.addResourceHandler("/covers/**")
                .addResourceLocations(uploadPath + "covers/");
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(uploadPath + "avatars/");

        System.out.println("✅ 静态资源映射完成:");
        System.out.println("    /images/** -> " + uploadPath + "images/");
    }

        @PostConstruct
    public void init() {
        System.out.println("✅ StaticResourceConfig 已加载");
        File testFile = new File("/root/voyagemate/new-voyage-mate/uploads/images/destinations/shenzhen.jpg");
        System.out.println("📂 测试图片存在？ " + testFile.exists());
    }
}
