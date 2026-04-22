package org.ershoupingtai.common.security;

import io.jsonwebtoken.Claims;
import org.ershoupingtai.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${app.offline-user.force:true}")
    private boolean forceOfflineUser;

    @Value("${app.offline-user.student-id:20260001}")
    private String offlineStudentId;

    @Value("${app.offline-user.username:离线演示账号}")
    private String offlineUsername;

    private final AuthTokenService authTokenService;

    public JwtInterceptor(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (forceOfflineUser) {
            // 离线演示模式下放行受保护接口，避免本地联调被鉴权阻塞。
            UserContext.set(offlineStudentId, offlineUsername);
            return true;
        }

        // 用户中心接口统一要求 Bearer access token。
        String authHeader = request.getHeader(AUTH_HEADER);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, ResultCode.TOKEN_MISSING);
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = authTokenService.validateAccessToken(token);
            String userId = claims.getSubject();
            Object userNameValue = claims.get("userName");
            String userName = userNameValue == null ? null : String.valueOf(userNameValue);
            if (!StringUtils.hasText(userId)) {
                writeUnauthorized(response, ResultCode.TOKEN_INVALID);
                return false;
            }

            // 通过 ThreadLocal 传递当前登录态，供控制器直接使用。
            UserContext.set(userId, userName);
            return true;
        } catch (IllegalArgumentException ex) {
            writeUnauthorized(response, mapError(ex.getMessage()));
            return false;
        } catch (Exception ex) {
            writeUnauthorized(response, ResultCode.TOKEN_INVALID);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":" + resultCode.getCode() + ",\"msg\":\"" + resultCode.getMsg() + "\",\"data\":null}");
    }

    private ResultCode mapError(String error) {
        // 统一把内部错误标识映射为可稳定消费的业务错误码。
        if ("token_missing".equals(error)) {
            return ResultCode.TOKEN_MISSING;
        }
        if ("token_expired".equals(error)) {
            return ResultCode.TOKEN_EXPIRED;
        }
        if ("token_revoked".equals(error)) {
            return ResultCode.TOKEN_REVOKED;
        }
        if ("token_invalid".equals(error)) {
            return ResultCode.TOKEN_INVALID;
        }
        return ResultCode.UNAUTHORIZED;
    }
}
