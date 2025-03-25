package com.simple_online_store_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    //RedisConnectionFactory — интерфейс, который умеет устанавливать соединения с Redis.
    public RedisConnectionFactory redisConnectionFactory() {
        //LettuceConnectionFactory() — реализация через Lettuce, современный асинхронный Redis-клиент на базе Netty.
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("redis", 6379);
        return new LettuceConnectionFactory(config); // можно заменить на Jedis, если используешь его
        //JedisConnectionFactory, синхронный, более старый (его используют реже в новых проектах)
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        //Создаём объект RedisTemplate, через который будем читать/записывать данные в Redis.
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory); //Подключаем шаблон к Redis-соединению (из первого бина).
        //Указываем, что ключи должны храниться как обычные строки.
        // Это нужно, чтобы в Redis ключ выглядел как "username" (а не двоичный набор байтов).
        template.setKeySerializer(new StringRedisSerializer()); // сериализатор ключей
        template.setValueSerializer(new StringRedisSerializer()); // сериализатор значений
        return template;
    }
}
