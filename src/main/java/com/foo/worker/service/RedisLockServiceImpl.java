package com.foo.worker.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

import reactor.core.publisher.Mono;

/**
 * RedisLockServiceImpl: Implements distributed lock management using Redis
 * to ensure that only one instance processes a specific order at a time.
 *
 * Main Responsibilities:
 * - Acquire and release locks using Redis.
 * - Prevent concurrent processing of the same order.
 *
 * Details:
 * - Locks are stored with keys in the format `lock_order:<orderId>`.
 * - Each lock has an expiration timeout to prevent permanent blocking.
 *
 * Error Handling:
 * - If Redis connection fails, a Mono.error or default false is returned.
 * 
 * @author Freyder Otalvaro
 * @version 1.0
 * @since 2024-10-17
 */
@Service
public class RedisLockServiceImpl implements RedisLockService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Constructor that initializes the reactive Redis template for lock operations.
     *
     * @param redisTemplate Reactive Redis template used to manage lock operations.
     */
    public RedisLockServiceImpl(@Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to acquire a lock in Redis for the specified order.
     * Ensures that only one instance of the worker processes the order at a time.
     *
     * @param orderId The unique identifier of the order.
     * @return Mono<Boolean> indicating whether the lock was successfully acquired.
     */
    @Override
    public Mono<Boolean> acquireLock(String orderId) {
        return redisTemplate.opsForValue().setIfAbsent("lock_order:" + orderId, "LOCKED")
                .flatMap(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        System.out.println("Lock acquired for order: " + orderId);

                        // Set expiration time for the lock to avoid deadlocks
                        return redisTemplate.expire("lock_order:" + orderId, Duration.ofMinutes(5))
                                .thenReturn(true);
                    } else {
                        System.out.println("Failed to acquire lock for order: " + orderId);
                        return Mono.just(false);
                    }
                });
    }

    /**
     * Releases the lock in Redis for the given order ID after processing is complete.
     *
     * @param orderId The ID of the order to unlock.
     * @return Mono<Boolean> indicating whether the lock was successfully released.
     */
    @Override
    public Mono<Boolean> releaseLock(String orderId) {
        if (orderId == null) {
            return Mono.just(false);
        }

        return redisTemplate.delete("lock_order:" + orderId)
                .map(deleted -> deleted > 0)
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.just(false));
    }
}
