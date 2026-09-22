package com.tdtech.linkx.node.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

/**
 * P2P节点JWT工具类
 */
@Slf4j
@Component
public class P2pJwtUtil {

    @Value("${p2p.jwt.secret}")
    private String secret;

    @Value("${p2p.jwt.expire-seconds}")
    private long expireSeconds;

    private Key signingKey;

    @PostConstruct
    public void init() {
        // 确保密钥至少256位（32字节）用于HS256
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, keyBytes.length);
            keyBytes = paddedKey;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成JWT Token
     *
     * @param peerId    节点标识
     * @param ip        IP地址
     * @param port      端口
     * @param challenge 挑战码
     * @return JWT Token
     */
    public String generateToken(String peerId, String ip, int port, String challenge) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setHeaderParam("alg", "HS256")
                .setHeaderParam("typ", "JWT")
                .claim("peerId", peerId)
                .claim("ip", ip)
                .claim("port", port)
                .claim("challenge", challenge)
                .claim("role", "peer")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireSeconds * 1000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析JWT Token
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 验证JWT Token
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 生成开放数据请求Token
     *
     * @param peerId 请求目标节点ID
     * @return JWT Token
     */
    public String generateOpenDataToken(String peerId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setHeaderParam("alg", "HS256")
                .setHeaderParam("typ", "JWT")
                .claim("peerId", peerId)
                .claim("type", "opendata")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireSeconds * 1000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成 dispatch 调对端 proxy 的 Token。
     * 携带本端 peerId，对端 proxy 校验 peerId 匹配。
     *
     * @param localPeerId 本端 peerId
     * @return JWT Token
     */
    public String generateDispatchToken(String localPeerId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .setHeaderParam("alg", "HS256")
                .setHeaderParam("typ", "JWT")
                .claim("peerId", localPeerId)
                .claim("type", "dispatch")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireSeconds * 1000))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 Token 中提取 peerId（不校验过期时间，仅解析）。
     */
    public String getPeerIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("peerId", String.class);
        } catch (Exception e) {
            log.warn("Failed to extract peerId from token: {}", e.getMessage());
            return null;
        }
    }

}
