package com.simple_online_store_backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {
    // RedisTemplate — бин, через который Spring взаимодействует с Redis
    private final RedisTemplate<String, String> redisTemplate;
    private final static long REFRESH_TOKEN_EXPIRATION_MINUTES = 60 * 24 * 7;

    public RefreshTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Сохраняем refresh токен в Redis по username с истечением срока
    public void saveRefreshToken(String username, String refreshToken) {
        redisTemplate.opsForValue() // способ работы с Redis, когда ты хочешь использовать простые пары ключ–значение.
        // Это аналог обычного Map (например, "user1" -> "refresh_token").
                .set(username, refreshToken, REFRESH_TOKEN_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        //TimeUnit.MINUTES — это единица измерения времени, которую использует Redis при установке TTL (время жизни ключа).
        //Вместе с REFRESH_TOKEN_EXPIRATION_MINUTES говорит: «удалить refresh token через 10080 минут» (7 дней).
    }

    // Получаем refresh токен по username
    public String getRefreshToken(String username) {
        return redisTemplate.opsForValue().get(username);
    }

    // Удаляем refresh токен по username (например, при logout)
    public void deleteRefreshToken(String username) {
        redisTemplate.delete(username);
    }
}
