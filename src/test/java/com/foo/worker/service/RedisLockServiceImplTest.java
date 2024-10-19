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

public class RedisLockServiceImplTest {

 @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations; // Mock para opsForValue()

    @InjectMocks
    private RedisLockServiceImpl redisLockService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Simular que opsForValue() retorna valueOperations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    public void testAcquireLock_Successful() {
        // Simular que el lock fue adquirido correctamente
        when(valueOperations.setIfAbsent(any(String.class), any(String.class))).thenReturn(Mono.just(true));
        when(redisTemplate.expire(any(String.class), any())).thenReturn(Mono.just(true));

        StepVerifier.create(redisLockService.acquireLock("order-123"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testAcquireLock_Unsuccessful() {
        // Simular que el lock no fue adquirido
        when(valueOperations.setIfAbsent(any(String.class), any(String.class))).thenReturn(Mono.just(false));

        StepVerifier.create(redisLockService.acquireLock("order-123"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    public void testReleaseLock_Successful() {
        // Simular que el lock fue liberado correctamente
        when(redisTemplate.delete(any(String.class))).thenReturn(Mono.just(1L)); // 1L indica que la clave fue eliminada

        StepVerifier.create(redisLockService.releaseLock("order-123"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    public void testReleaseLock_Unsuccessful() {
        // Simular que el lock no fue liberado
        when(redisTemplate.delete(any(String.class))).thenReturn(Mono.just(0L)); // 0L indica que no se eliminó ninguna clave

        StepVerifier.create(redisLockService.releaseLock("order-123"))
                .expectNext(false)
                .verifyComplete();
    }

}