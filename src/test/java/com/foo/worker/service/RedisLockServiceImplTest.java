package com.foo.worker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for RedisLockServiceImpl using Mockito and StepVerifier.
 *
 * This test class validates:
 * - Successful and unsuccessful lock acquisition
 * - Successful and unsuccessful lock release
 *
 * Author: Freyder Otalvaro
 */
public class RedisLockServiceImplTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations; // Mock for opsForValue()

    @InjectMocks
    private RedisLockServiceImpl redisLockService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Simulate that opsForValue() returns the mocked valueOperations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void testAcquireLock_Successful() {
        // Simulate successful lock acquisition
        when(valueOperations.setIfAbsent(any(String.class), any(String.class))).thenReturn(Mono.just(true));
        when(redisTemplate.expire(any(String.class), any())).thenReturn(Mono.just(true));

        StepVerifier.create(redisLockService.acquireLock("order-123"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testAcquireLock_Unsuccessful() {
        // Simulate lock not acquired
        when(valueOperations.setIfAbsent(any(String.class), any(String.class))).thenReturn(Mono.just(false));

        StepVerifier.create(redisLockService.acquireLock("order-123"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    public void testReleaseLock_Successful() {
        // Simulate successful lock release
        when(redisTemplate.delete(any(String.class))).thenReturn(Mono.just(1L)); // 1L means key was deleted

        StepVerifier.create(redisLockService.releaseLock("order-123"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testReleaseLock_Unsuccessful() {
        // Simulate lock not released
        when(redisTemplate.delete(any(String.class))).thenReturn(Mono.just(0L)); // 0L means no key was deleted

        StepVerifier.create(redisLockService.releaseLock("order-123"))
                .expectNext(false)
                .verifyComplete();
    }
}
