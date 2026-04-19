package org.ershoupingtai.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    // 双令牌类型标识：access 用于接口访问，refresh 用于换发新 access。
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.access-expire-seconds}")
    private long accessExpireSeconds;

    @Value("${jwt.refresh-expire-seconds}")
    private long refreshExpireSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, String userName, String jti) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + accessExpireSeconds * 1000);
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TYPE_ACCESS);
        claims.put("userName", userName);
        claims.put("jti", jti);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String userId, String userName, String jti) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + refreshExpireSeconds * 1000);
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TYPE_REFRESH);
        claims.put("userName", userName);
        claims.put("jti", jti);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expireAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        // 所有提取方法统一依赖该解析逻辑，保证签名校验路径一致。
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isValid(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String extractUserName(String token) {
        Object value = parseToken(token).get("userName");
        return value == null ? null : String.valueOf(value);
    }

    public String extractJti(String token) {
        Object value = parseToken(token).get("jti");
        return value == null ? null : String.valueOf(value);
    }

    public String extractType(String token) {
        Object value = parseToken(token).get("type");
        return value == null ? null : String.valueOf(value);
    }

    public Date extractIssuedAt(String token) {
        return parseToken(token).getIssuedAt();
    }

    public Date extractExpiration(String token) {
        return parseToken(token).getExpiration();
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }

    public long getRefreshExpireSeconds() {
        return refreshExpireSeconds;
    }
}
