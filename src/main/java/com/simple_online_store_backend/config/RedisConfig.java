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
    //RedisConnectionFactory - interface for establishing a connection to Redis.
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("redis", 6379);
        //LettuceConnectionFactory() — implementation via Lettuce, a modern asynchronous Redis client based on Netty.
        return new LettuceConnectionFactory(config);
        // LettuceConnectionFactory can be replaced with JedisConnectionFactory - synchronous, older
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        // RedisTemplate - allows you to read/write data to Redis.
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory); // Connect the template to the Redis connection (from the first bean).
        /*
        Explanation:
            Specify that keys should be stored as regular strings
            - so that in Redis the key looks like "username" (and not a binary set of bytes).
         */
        template.setKeySerializer(new StringRedisSerializer()); // Specifies the serializer for Redis keys.
        template.setValueSerializer(new StringRedisSerializer()); // Specifies the serializer for Redis values.
        return template;
    }
}
