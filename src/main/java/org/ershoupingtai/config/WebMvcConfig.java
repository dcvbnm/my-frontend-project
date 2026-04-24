package org.ershoupingtai.config;

import org.ershoupingtai.common.security.JwtInterceptor;
import org.ershoupingtai.config.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final LoginInterceptor loginInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor, LoginInterceptor loginInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传文件映射
        String uploadPath = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // LoginInterceptor - 拦截页面请求，检查登录状态
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/refresh",
                        "/css/**",
                        "/js/**",
                        "/uploads/**",
                        "/error",
                        "/favicon.ico"
                );

        // JwtInterceptor - 拦截 API 请求，进行 JWT 鉴权
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/user/center/**", "/api/user/logout")
                .excludePathPatterns(
                        "/api/user/login", 
                        "/api/user/register", 
                        "/api/user/refresh"
                );
    }
}