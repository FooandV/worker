package com.foo.worker.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * RedisFailureService: Manages the storage and retrieval of failed order messages
 * along with their attempt counts in Redis.
 *
 * Responsibilities:
 * - Stores a failed order message along with the number of processing attempts.
 * - Retrieves the failed message and attempt count for further analysis or retries.
 *
 * Main methods:
 * - storeFailedMessage: Stores the failed order message and the number of attempts.
 * - getFailedMessage: Retrieves the failed message by order ID.
 * - getAttemptCount: Retrieves the number of attempts made for the failed order.
 */
@Service
public class RedisFailureService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RedisFailureService(@Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Stores a failed order message and the number of retry attempts in Redis.
     *
     * @param orderId The ID of the failed order.
     * @param message The original order message.
     * @param attempt The number of attempts made to process the order.
     * @return Mono<Boolean> indicating the result of the operation.
     */
    public Mono<Boolean> storeFailedMessage(String orderId, String message, int attempt) {
        return redisTemplate.opsForValue().set("failed_order:" + orderId, message)
                .flatMap(result -> redisTemplate.opsForValue().set("failed_attempts:" + orderId,
                        String.valueOf(attempt)));
    }

    /**
     * Retrieves the failed message from Redis.
     *
     * @param orderId The ID of the failed order.
     * @return Mono<String> containing the failed message.
     */
    public Mono<String> getFailedMessage(String orderId) {
        return redisTemplate.opsForValue().get("failed_order:" + orderId);
    }

    /**
     * Retrieves the number of retry attempts for a failed order.
     *
     * @param orderId The ID of the failed order.
     * @return Mono<Integer> with the attempt count.
     */
    public Mono<Integer> getAttemptCount(String orderId) {
        return redisTemplate.opsForValue().get("failed_attempts:" + orderId)
                .map(Integer::parseInt);
    }
}
