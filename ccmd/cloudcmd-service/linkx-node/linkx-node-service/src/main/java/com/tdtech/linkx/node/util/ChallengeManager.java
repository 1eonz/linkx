package com.tdtech.linkx.node.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 挑战码管理器
 */
@Slf4j
@Component
public class ChallengeManager {

    private static final String PREFIX = "p2p:challenge:";

    @Value("${p2p.challenge.expire-seconds:30}")
    private long expireSeconds;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom;

    public ChallengeManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.secureRandom = new SecureRandom();
    }

    /**
     * 生成挑战码
     *
     * @param peerId 节点标识
     * @return 挑战码
     */
    public String generateChallenge(String peerId) {
        // 生成128位随机挑战码
        byte[] challengeBytes = new byte[16];
        secureRandom.nextBytes(challengeBytes);
        String challenge = bytesToHex(challengeBytes);

        String key = PREFIX + peerId;
        redisTemplate.opsForValue().set(key, challenge, expireSeconds, TimeUnit.SECONDS);

        log.info("Generated challenge for peerId: {}, challenge: {}, expire: {}s", peerId, challenge, expireSeconds);
        return challenge;
    }

    /**
     * 验证并消费挑战码
     *
     * @param peerId    节点标识
     * @param challenge 挑战码
     * @return 是否验证成功
     */
    public boolean verifyAndConsume(String peerId, String challenge) {
        String key = PREFIX + peerId;
        String stored = redisTemplate.opsForValue().get(key);

        if (stored != null && stored.equals(challenge)) {
            // 验证成功后删除挑战码（一次性使用）
            redisTemplate.delete(key);
            log.debug("Challenge verified and consumed for peerId: {}", peerId);
            return true;
        }

        log.warn("Challenge verification failed for peerId: {}, expected: {}, actual: {}", peerId, stored, challenge);
        return false;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
