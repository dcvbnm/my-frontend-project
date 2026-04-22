package org.ershoupingtai.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthTokenService {
    // Access token 即时失效依赖黑名单键（TTL 与 token 剩余有效期一致）。
    private static final String ACCESS_BLACKLIST_PREFIX = "auth:at:blacklist:";
    // Refresh token 按“用户+设备”维护活跃会话。
    private static final String REFRESH_ACTIVE_PREFIX = "auth:rt:active:";
    // 设备索引用于全端退出时定点删除，避免使用 keys 扫描。
    private static final String REFRESH_DEVICE_SET_PREFIX = "auth:rt:devices:";
    private static final String REFRESH_BLACKLIST_PREFIX = "auth:rt:blacklist:";
    // 用户级撤销时间戳用于改密/强退后的全端立即失效。
    private static final String USER_REVOKE_PREFIX = "auth:user:revoke:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.offline-user.disable-token-store:true}")
    private boolean disableTokenStore;

    public AuthTokenService(JwtUtil jwtUtil, StringRedisTemplate redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    public TokenBundle issueTokens(String userId, String userName, String deviceId) {
        String normalizedDeviceId = normalizeDeviceId(deviceId);
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        TokenBundle bundle = new TokenBundle();
        bundle.setAccessToken(jwtUtil.generateAccessToken(userId, userName, accessJti));
        bundle.setRefreshToken(jwtUtil.generateRefreshToken(userId, userName, refreshJti));
        bundle.setAccessExpiresIn(jwtUtil.getAccessExpireSeconds());
        bundle.setRefreshExpiresIn(jwtUtil.getRefreshExpireSeconds());

        if (disableTokenStore) {
            return bundle;
        }

        redisTemplate.opsForValue().set(
                refreshActiveKey(userId, normalizedDeviceId),
                refreshJti,
                Duration.ofSeconds(jwtUtil.getRefreshExpireSeconds())
        );
        redisTemplate.opsForSet().add(refreshDeviceSetKey(userId), normalizedDeviceId);
        redisTemplate.expire(refreshDeviceSetKey(userId), Duration.ofSeconds(jwtUtil.getRefreshExpireSeconds()));
        return bundle;
    }

    public TokenBundle refreshTokens(String refreshToken, String deviceId) {
        if (!jwtUtil.isValid(refreshToken)) {
            throw new IllegalArgumentException("refresh_token_expired");
        }
        Claims claims = jwtUtil.parseToken(refreshToken);
        String tokenType = jwtUtil.extractType(refreshToken);
        if (!JwtUtil.TYPE_REFRESH.equals(tokenType)) {
            throw new IllegalArgumentException("refresh_token_invalid");
        }

        String userId = claims.getSubject();
        String refreshJti = jwtUtil.extractJti(refreshToken);
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(refreshJti)) {
            throw new IllegalArgumentException("refresh_token_invalid");
        }

        String normalizedDeviceId = normalizeDeviceId(deviceId);
        Object userNameValue = claims.get("userName");
        String userName = userNameValue == null ? userId : String.valueOf(userNameValue);
        if (disableTokenStore) {
            return issueTokens(userId, userName, normalizedDeviceId);
        }

        if (isRevokedUser(userId, claims.getIssuedAt())) {
            throw new IllegalArgumentException("token_revoked");
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(refreshBlacklistKey(refreshJti)))) {
            throw new IllegalArgumentException("refresh_token_revoked");
        }

        String activeRefreshJti = redisTemplate.opsForValue().get(refreshActiveKey(userId, normalizedDeviceId));
        if (!refreshJti.equals(activeRefreshJti)) {
            // 说明该设备已完成 refresh 轮换，旧 refresh token 禁止再次使用。
            throw new IllegalArgumentException("refresh_token_replaced");
        }

        long ttlSeconds = secondsToExpire(claims.getExpiration());
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(refreshBlacklistKey(refreshJti), "1", ttlSeconds, TimeUnit.SECONDS);
        }

        return issueTokens(userId, userName, normalizedDeviceId);
    }

    public Claims validateAccessToken(String accessToken) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(accessToken);
        } catch (ExpiredJwtException ex) {
            throw new IllegalArgumentException("token_expired");
        } catch (Exception ex) {
            throw new IllegalArgumentException("token_invalid");
        }

        Object typeValue = claims.get("type");
        String tokenType = typeValue == null ? null : String.valueOf(typeValue);
        if (!JwtUtil.TYPE_ACCESS.equals(tokenType)) {
            throw new IllegalArgumentException("token_invalid");
        }

        Object jtiValue = claims.get("jti");
        String jti = jtiValue == null ? null : String.valueOf(jtiValue);
        if (!disableTokenStore
                && StringUtils.hasText(jti)
                && Boolean.TRUE.equals(redisTemplate.hasKey(accessBlacklistKey(jti)))) {
            throw new IllegalArgumentException("token_revoked");
        }

        String userId = claims.getSubject();
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("token_invalid");
        }

        if (isRevokedUser(userId, claims.getIssuedAt())) {
            // 改密或全端退出后，旧 token 会因签发时间早于撤销时间而失效。
            throw new IllegalArgumentException("token_revoked");
        }

        return claims;
    }

    public void revokeAllDevices(String userId, String accessToken) {
        if (!StringUtils.hasText(userId) || disableTokenStore) {
            return;
        }

        redisTemplate.opsForValue().set(
                userRevokeKey(userId),
                String.valueOf(System.currentTimeMillis()),
                Duration.ofSeconds(jwtUtil.getRefreshExpireSeconds())
        );

        Set<String> devices = redisTemplate.opsForSet().members(refreshDeviceSetKey(userId));
        if (devices != null && !devices.isEmpty()) {
            // 使用设备索引逐个删除活跃 refresh，避免全库模式匹配扫描。
            for (String device : devices) {
                redisTemplate.delete(refreshActiveKey(userId, device));
            }
        }
        redisTemplate.delete(refreshDeviceSetKey(userId));

        if (StringUtils.hasText(accessToken) && jwtUtil.isValid(accessToken)) {
            String type = jwtUtil.extractType(accessToken);
            if (JwtUtil.TYPE_ACCESS.equals(type)) {
                String jti = jwtUtil.extractJti(accessToken);
                long ttlSeconds = secondsToExpire(jwtUtil.extractExpiration(accessToken));
                if (StringUtils.hasText(jti) && ttlSeconds > 0) {
                    redisTemplate.opsForValue().set(accessBlacklistKey(jti), "1", ttlSeconds, TimeUnit.SECONDS);
                }
            }
        }
    }

    private boolean isRevokedUser(String userId, Date issuedAt) {
        if (disableTokenStore) {
            return false;
        }
        String revokeTime = redisTemplate.opsForValue().get(userRevokeKey(userId));
        if (!StringUtils.hasText(revokeTime)) {
            return false;
        }
        if (issuedAt == null) {
            return true;
        }

        try {
            long revokeMillis = Long.parseLong(revokeTime);
            return issuedAt.getTime() < revokeMillis;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private long secondsToExpire(Date expireAt) {
        if (expireAt == null) {
            return 0;
        }
        long diff = (expireAt.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(diff, 0);
    }

    private String normalizeDeviceId(String deviceId) {
        if (!StringUtils.hasText(deviceId)) {
            return "default";
        }
        return deviceId.trim();
    }

    private String accessBlacklistKey(String jti) {
        return ACCESS_BLACKLIST_PREFIX + jti;
    }

    private String refreshBlacklistKey(String jti) {
        return REFRESH_BLACKLIST_PREFIX + jti;
    }

    private String refreshActiveKey(String userId, String deviceId) {
        return REFRESH_ACTIVE_PREFIX + userId + ":" + deviceId;
    }

    private String userRevokeKey(String userId) {
        return USER_REVOKE_PREFIX + userId;
    }

    private String refreshDeviceSetKey(String userId) {
        return REFRESH_DEVICE_SET_PREFIX + userId;
    }
}
