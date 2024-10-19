package com.foo.worker.service;

import reactor.core.publisher.Mono;

/**
 * RedisLockService: Interfaz que define los métodos para gestionar locks (bloqueos) distribuidos utilizando Redis.
 * Esta interfaz asegura que solo una instancia del worker pueda procesar un pedido en un momento dado,
 * evitando la concurrencia en el procesamiento de un mismo pedido.
 * 
 * Responsabilidades:
 * - Proveer métodos para adquirir(acquireLock) y liberar(releaseLock) locks para pedidos.
 * @author Freyder Otalvaro
 * @version 1.0
 * @since 2024-10-17
 */
public interface RedisLockService {

    Mono<Boolean> acquireLock(String orderId);
    Mono<Boolean> releaseLock(String orderId);

}
