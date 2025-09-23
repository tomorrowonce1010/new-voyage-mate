package com.se_07.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // classpath static resources (原有映射)
        String classpathLocation = "classpath:/static/";
        // 外部上传目录，注意路径必须以 file: 开头
        String uploadLocation = "file:uploads/";

        // 由于应用配置了context-path=/api，所以静态资源路径也需要加上/api前缀
        registry.addResourceHandler("/static/**")
                .addResourceLocations(classpathLocation, uploadLocation);

        registry.addResourceHandler("/covers/**")
                .addResourceLocations("file:uploads/covers/");

        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:uploads/avatars/");
                
        // 添加images路径映射，支持destinations图片访问
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/images/");
                
        // 添加API路径的静态资源映射（与context-path一致）
        registry.addResourceHandler("/api/static/**")
                .addResourceLocations(classpathLocation, uploadLocation);
                
        registry.addResourceHandler("/api/covers/**")
                .addResourceLocations("file:uploads/covers/");
                
        registry.addResourceHandler("/api/avatars/**")
                .addResourceLocations("file:uploads/avatars/", "classpath:/static/avatars/");
                
        // 添加API路径的images映射
        registry.addResourceHandler("/api/images/**")
                .addResourceLocations("file:uploads/images/");
                
        // 添加调试日志
        System.out.println("静态资源配置已加载:");
        System.out.println("- /static/** -> " + classpathLocation + ", " + uploadLocation);
        System.out.println("- /covers/** -> file:uploads/covers/");
        System.out.println("- /avatars/** -> file:uploads/avatars/");
        System.out.println("- /images/** -> file:uploads/images/");
        System.out.println("- /api/static/** -> " + classpathLocation + ", " + uploadLocation);
        System.out.println("- /api/covers/** -> file:uploads/covers/");
        System.out.println("- /api/avatars/** -> file:uploads/avatars/, classpath:/static/avatars/");
        System.out.println("- /api/images/** -> file:uploads/images/");
    }
} 