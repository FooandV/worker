package com.foo.worker.service;

import org.springframework.stereotype.Service;

import com.foo.worker.models.CustomerDetails;
import com.foo.worker.models.Order;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.models.ProductDetails;

import reactor.core.publisher.Mono;

/**
 * OrderProcessorServiceImpl: Procesa los mensajes de pedidos recibidos de Kafka.
 * 
 * Funcionalidad principal:
 * - Enriquecimiento de datos de clientes y productos a través de APIs externas (Go).
 * - Almacenamiento de pedidos enriquecidos en MongoDB.
 * - Gestión de concurrencia con Redis para evitar procesamiento duplicado.
 * 
 * Manejo de Errores:
 * - Reintentos automáticos con Resilience4j para fallos en las APIs.
 * - Uso de locks distribuidos en Redis para asegurar procesamiento único de pedidos.
 * 
 * Dependencias:
 * - EnrichmentService: Enriquecimiento de datos de clientes y productos.
 * - OrderStorageService: Almacenamiento en MongoDB.
 * - RedisLockService: Gestión de locks con Redis.
 * 
 * @author Freyder Otalvaro
 * @version 1.2
 * @since 2024-10-19
 */
@Service
public class OrderProcessorServiceImpl implements OrderProcessorService {

    private final EnrichmentService enrichmentService;
    private final OrderStorageService orderStorageService;
    private final RedisLockService redisLockService; // Añadir RedisLockService

    // constructor que inicializa los servicios necesarios para procesar pedidos.
    public OrderProcessorServiceImpl(EnrichmentService enrichmentService,
            OrderStorageService orderStorageService,
            RedisLockService redisLockService) {
        this.enrichmentService = enrichmentService;
        this.orderStorageService = orderStorageService;
        this.redisLockService = redisLockService;
    }

    /**
     * Método para procesar un mensaje de pedido recibido desde Kafka.
     * Se realiza el enriquecimiento de datos de cliente y producto, y el pedido
     * se almacena en MongoDB si las validaciones son exitosas.
     * @param orderMessage Objeto que contiene los datos básicos del pedido recibido desde Kafka.
     * @return Mono<Order> Un flujo reactivo que representa el resultado del procesamiento del pedido.
     * @throws RuntimeException Si el cliente está inactivo o el producto no se encuentra.
     */
    @Override
    public Mono<Order> processOrder(OrderMessage orderMessage) {
        // Intentar adquirir el lock(bloqueo) antes de procesar el pedido
        return redisLockService.acquireLock(orderMessage.getOrderId())
                .flatMap(acquired -> {
                    if (!acquired) {
                        // Si no se pudo adquirir el lock(bloqueo), detener el proceso
                        return Mono.error(new RuntimeException("Pedido ya está siendo procesado"));
                    }
                    // Si se adquiere el lock(bloqueo), proceder con el enriquecimiento
                    return enrichmentService.enrichCustomerWithResilience(orderMessage)
                            .zipWith(enrichmentService.enrichProductWithResilience(orderMessage))
                            .flatMap(tuple -> {
                                CustomerDetails customer = tuple.getT1();
                                ProductDetails product = tuple.getT2();

                                if (!customer.getActive()) {
                                    return Mono.error(new RuntimeException("Cliente inactivo"));
                                }

                                if (product == null || product.getProductId() == null) {
                                    return Mono.error(new RuntimeException("Producto no encontrado"));
                                }

                                System.out
                                        .println("Datos enriquecidos: Cliente: " + customer + ", Producto: " + product);
                                Order order = createEnrichedOrder(orderMessage, customer, product);
                                return orderStorageService.saveOrder(order);
                            })
                            .doFinally(signalType -> {
                                redisLockService.releaseLock(orderMessage.getOrderId())
                                    .defaultIfEmpty(false) // Asegura que no sea nulo
                                    .subscribe(success -> {
                                        if (!success) {
                                            System.out.println("No se pudo liberar el lock(bloqueo) para el pedido: " + orderMessage.getOrderId());
                                        } else {
                                            System.out.println("Lock(bloqueo) liberado para el pedido: " + orderMessage.getOrderId());
                                        }
                                    }, error -> {
                                        System.out.println("Error al liberar el lock(bloqueo): " + error.getMessage());
                                    });
                            });
                });
    }

    /**
     * Método que crea un objeto Order enriquecido con los datos del mensaje de
     * Kafka, y los detalles del cliente y producto recibidos de las APIs.
     * @param orderMessage Objeto con la información básica del pedido.
     * @param customer     Objeto con los detalles del cliente.
     * @param product      Objeto con los detalles del producto.
     * @return Order Un objeto de pedido completo listo para ser almacenado.
     */
    @Override
    public Order createEnrichedOrder(OrderMessage orderMessage, CustomerDetails customer, ProductDetails product) {
        Order order = new Order();
        order.setOrderId(orderMessage.getOrderId());
        order.setCustomerId(customer.getCustomerId());
        order.setProducts(orderMessage.getProducts());
        return order;
    }

}
