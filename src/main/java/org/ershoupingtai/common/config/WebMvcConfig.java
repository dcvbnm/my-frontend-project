package org.ershoupingtai.common.config;

import org.ershoupingtai.common.security.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户中心受保护接口走 JWT 鉴权；登录/注册/刷新接口必须放行。
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/user/center/**", "/api/user/logout")
                .excludePathPatterns("/api/user/login", "/api/user/register", "/api/user/refresh");
    }
}
