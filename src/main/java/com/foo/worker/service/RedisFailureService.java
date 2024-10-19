package com.foo.worker.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * La clase RedisFailureService gestiona el almacenamiento y la recuperación de mensajes de pedidos fallidos
 * junto con el número de intentos en Redis.
 * 
 * Este servicio se encarga de:
 * - Almacenar un pedido fallido en Redis cuando el procesamiento del pedido no tiene éxito, junto con un contador de intentos.
 * - Recuperar el mensaje del pedido fallido y el número de intentos desde Redis para su posterior análisis o reintento.
 * Métodos principales:
 * - storeFailedMessage: Almacena un mensaje de pedido fallido y el número de intentos en Redis.
 * - getFailedMessage: Recupera el mensaje de un pedido fallido desde Redis.
 * - getAttemptCount: Recupera el número de intentos realizados para procesar el pedido fallido desde Redis.
 */
@Service
public class RedisFailureService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;


    public RedisFailureService( @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> storeFailedMessage(String orderId, String message, int attempt) {
        
        return redisTemplate.opsForValue().set("failed_order:" + orderId, message)
                .flatMap(result -> redisTemplate.opsForValue().set("failed_attempts:" + orderId,
                        String.valueOf(attempt)));
    }

    public Mono<String> getFailedMessage(String orderId) {
        return redisTemplate.opsForValue().get("failed_order:" + orderId);
    }

    public Mono<Integer> getAttemptCount(String orderId) {
        return redisTemplate.opsForValue().get("failed_attempts:" + orderId)
                .map(Integer::parseInt); 
    }
}
