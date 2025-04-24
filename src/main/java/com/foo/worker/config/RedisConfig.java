package com.foo.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * Configuration class that sets up a reactive RedisTemplate 
 * for non-blocking Redis operations using String keys and values.
 */
@Configuration
public class RedisConfig {

    /**
     * Defines a reactive RedisTemplate using String serialization.
     * This is used to interact with Redis in a reactive, non-blocking way.
     */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisTemplate<>(factory, RedisSerializationContext.string());
    }

}
