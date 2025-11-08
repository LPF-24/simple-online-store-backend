package com.simple_online_store_backend.unit.service;

import com.simple_online_store_backend.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTests {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(redisTemplate);
    }

    @Test
    void shouldSaveRefreshToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshTokenService.saveRefreshToken("john", "token123");

        verify(valueOperations).set(eq("john"), eq("token123"), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    void shouldGetRefreshToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("john")).thenReturn("token123");

        String result = refreshTokenService.getRefreshToken("john");

        assertEquals("token123", result);
    }

    @Test
    void shouldDeleteRefreshToken() {
        refreshTokenService.deleteRefreshToken("john");

        verify(redisTemplate).delete("john");
    }
}
