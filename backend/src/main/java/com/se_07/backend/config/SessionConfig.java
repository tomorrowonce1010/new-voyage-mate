package com.se_07.backend.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;

@Configuration
public class SessionConfig {
    
    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                // 配置session cookie
                SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
                sessionCookieConfig.setName("JSESSIONID");
                sessionCookieConfig.setHttpOnly(true);
                sessionCookieConfig.setSecure(false); // 开发环境设为false，生产环境设为true
                sessionCookieConfig.setMaxAge(24 * 60 * 60); // 24小时
                sessionCookieConfig.setPath("/");
                
                // 重要：设置domain为null，让浏览器根据端口自动隔离session
                // 这样不同端口的前端会有独立的session cookie
                sessionCookieConfig.setDomain(null);
                
                // 设置SameSite属性，确保跨端口请求时cookie能正确发送
                // 注意：这个设置在某些浏览器中可能不生效，我们会在拦截器中处理
            }
        };
    }
} 