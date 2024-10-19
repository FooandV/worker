package com.foo.worker.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

import reactor.core.publisher.Mono;

/**
 * RedisLockServiceImpl: Implementación del servicio que gestiona locks (bloqueos) distribuidos utilizando Redis.
 * Este servicio asegura que solo una instancia del worker procese un pedido en particular al mismo tiempo.
 * 
 * Responsabilidades:
 * - Gestionar el bloqueo y liberación de locks para pedidos utilizando Redis.
 * - Prevenir que múltiples instancias procesen el mismo pedido simultáneamente.
 * 
 * Dependencias:
 * - ReactiveRedisTemplate: Utiliza Redis como almacenamiento distribuido para gestionar los locks.
 * 
 * Detalles de Implementación:
 * - Los locks se manejan usando claves en Redis con el prefijo `lock_order:<orderId>`.
 * - Cada lock tiene un tiempo de expiración definido para evitar bloqueos eternos.
 * 
 * Manejo de Errores:
 * - En caso de fallos de conexión a Redis, se devuelve un `Mono.error` para evitar que el sistema falle silenciosamente.
 * 
 * @author Freyder Otalvaro
 * @version 1.0
 * @since 2024-10-17
 */
@Service
public class RedisLockServiceImpl implements RedisLockService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Constructor que inicializa el template de Redis para la gestión de locks.
     * @param redisTemplate Template reactivo de Redis utilizado para manejar las operaciones de lock.
     */
    public RedisLockServiceImpl(@Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Método que intenta adquirir un lock (bloqueo) en Redis para un pedido específico.
     * Este método asegura que solo una instancia del worker pueda procesar un pedido a la vez.
     * @param orderId El identificador único del pedido para el cual se intenta adquirir el lock.
     * @return Mono<Boolean> Un flujo reactivo que indica si el lock fue adquirido exitosamente (true) o no (false).
     */
    @Override
    public Mono<Boolean> acquireLock(String orderId) {
        // Intentar adquirir un lock(bloqueo) en Redis
        return redisTemplate.opsForValue().setIfAbsent("lock_order:" + orderId, "LOCKED")
                .flatMap(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        
                        System.out.println("Lock adquirido para el pedido: " + orderId);

                        // Si se adquirió el lock(bloqueo), establecer una expiración (timeout)
                        return redisTemplate.expire("lock_order:" + orderId, Duration.ofMinutes(5))
                                .thenReturn(true); // Lock adquirido exitosamente
                    } else {
                        System.out.println("No se pudo adquirir el lock para el pedido: " + orderId);
                        // Si no se pudo adquirir el lock, retorna false.
                        return Mono.just(false);
                    }
                });
    }

    /**
     * Método que libera el lock (bloqueo) en Redis una vez que el pedido ha sido procesado.
     * Esto asegura que otras instancias del worker puedan procesar el mismo pedido si es necesario.
     * @param orderId El identificador del pedido cuyo lock debe ser liberado.
     * @return Mono<Boolean> Un flujo reactivo que indica si el lock fue liberado exitosamente (true) o no (false).
     */
    @Override
    public Mono<Boolean> releaseLock(String orderId) {
        if (orderId == null) {
            // Evitar un error en caso de que se pase un valor nulo
            return Mono.just(false);
        }
        // Eliminar el lock(bloqueo) en Redis cuando ya no se necesita (cuando se termina de
        // procesar el pedido)
        return redisTemplate.delete("lock_order:" + orderId)
            .map(deleted -> deleted > 0) // Retorna true si se eliminó el lock, false en caso contrario
            .defaultIfEmpty(false) // Retorna false si la operación no produce un resultado
            .onErrorResume(e -> {
                // Manejar cualquier error que ocurra en la eliminación del lock
                return Mono.just(false);
            });
    }

}
