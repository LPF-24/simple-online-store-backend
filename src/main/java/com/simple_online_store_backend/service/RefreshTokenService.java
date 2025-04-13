package com.simple_online_store_backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {
    // RedisTemplate allows Spring to interact with Redis
    private final RedisTemplate<String, String> redisTemplate;
    private final static long REFRESH_TOKEN_EXPIRATION_MINUTES = 60 * 24 * 7;

    public RefreshTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Stores the refresh token in Redis using the username as the key.
    public void saveRefreshToken(String username, String refreshToken) {
        redisTemplate.opsForValue() // The way to work with Redis when you want to use simple key-value pairs.
                // Analogous to a regular Map (e.g. "user1" -> "refresh_token").
                .set(username, refreshToken, REFRESH_TOKEN_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        //TimeUnit.MINUTES is the time unit that Redis uses when setting the TTL (time to live of a key).
        //Together with REFRESH_TOKEN_EXPIRATION_MINUTES it means: "delete refresh token after 10080 minutes" (7 days).
    }

    // Get refresh token by username
    public String getRefreshToken(String username) {
        return redisTemplate.opsForValue().get(username);
    }

    // Delete refresh token by username (for example, when logging out)
    public void deleteRefreshToken(String username) {
        redisTemplate.delete(username);
    }
}
