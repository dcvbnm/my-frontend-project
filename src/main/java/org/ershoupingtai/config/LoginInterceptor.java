package org.ershoupingtai.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // 放行所有页面请求，让前端 JS 处理登录验证
        if (!uri.startsWith("/api/")) {
            return true;
        }
        
        // API 请求由 JwtInterceptor 处理，这里全部放行
        return true;
    }
}