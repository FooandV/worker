package com.foo.worker.service;

import reactor.core.publisher.Mono;

/**
 * RedisLockService: Interface that defines methods for managing distributed locks using Redis.
 * This interface ensures that only one instance of the worker can process a specific order at a time,
 * preventing concurrent processing of the same order.
 *
 * Responsibilities:
 * - Provide methods to acquire and release locks for order processing.
 * 
 * @author Freyder Otalvaro
 * @version 1.0
 * @since 2024-10-17
 */
public interface RedisLockService {

    Mono<Boolean> acquireLock(String orderId);
    Mono<Boolean> releaseLock(String orderId);

}
