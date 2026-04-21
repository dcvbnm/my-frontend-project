package org.ershoupingtai.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    public static final String LOGIN_USER_KEY = "LOGIN_USER";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object loginUser = request.getSession().getAttribute(LOGIN_USER_KEY);
        if (loginUser != null) {
            return true;
        }

        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\",\"data\":null}");
            return false;
        }

        response.sendRedirect("/login");
        return false;
    }
}
