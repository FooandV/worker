package com.foo.worker.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.service.OrderProcessorService;
import com.foo.worker.service.RedisFailureService;

import reactor.core.publisher.Mono;

/**
 * KafkaOrderConsumer: Esta clase se encarga de consumir los mensajes de pedidos 
 * desde un tópico de Kafka. Procesa los mensajes recibidos, los enriquece con información adicional 
 * sobre el cliente y los productos utilizando las APIs externas de GO, y almacena los pedidos procesados 
 * en MongoDB. En caso de que el pedido falle, maneja los intentos fallidos y los almacena en Redis para reintentos futuros.
 */
@Service
public class KafkaOrderConsumer {

    private final OrderProcessorService orderProcessorService;
    private final RedisFailureService redisFailureService;
    private final ObjectMapper objectMapper = new ObjectMapper(); 

    /**
     * Constructor de KafkaOrderConsumer.
     * @param orderProcessorService Servicio que se encarga del procesamiento y almacenamiento del pedido.
     * @param redisFailureService Servicio que maneja el almacenamiento y la recuperación de mensajes fallidos en Redis.
     */
    public KafkaOrderConsumer(OrderProcessorService orderProcessorService, RedisFailureService redisFailureService) {
        this.orderProcessorService = orderProcessorService;
        this.redisFailureService = redisFailureService;
    }

    /**
     * Método que escucha los mensajes del tópico "orders" de Kafka.
     * @param message El mensaje recibido desde Kafka en formato JSON.
     */
    @KafkaListener(topics = "orders", groupId = "order_group")
    public void consume(String message) {
        System.out.println("Pedido recibido:" + message);
        try {
            // convertimos  el mensaje JSON en un objeto OrderMessage
            OrderMessage orderMessage = objectMapper.readValue(message, OrderMessage.class);
            // Procesa el pedido utilizando el servicio de procesamiento de pedidos
            orderProcessorService.processOrder(orderMessage)
                    .subscribe(savedOrder -> {
                        System.out.println("Pedido almacenado en MongoDB con ID: " + savedOrder.getId());
                    }, error -> {
                        System.err.println("Error al procesar el pedido: " + error.getMessage());
                        // Maneja el fallo del pedido"order" y lo almacena en Redis para reintentos
                        handleFailedOrder(orderMessage, message);
                    });
        } catch (Exception e) {
            System.err.println("Error al procesar el mensaje: " + e.getMessage());
        }
    }
     /**
     * handleFailedOrder: Este método maneja los pedidos fallidos. Incrementa el número 
     * de intentos en Redis y, si se alcanza el máximo de intentos, deja de procesar el pedido.
     * @param orderMessage El pedido que falló al procesarse.
     * @param originalMessage El mensaje original recibido de Kafka.
     */
    private void handleFailedOrder(OrderMessage orderMessage, String originalMessage) {
        redisFailureService.getAttemptCount(orderMessage.getOrderId())
                .defaultIfEmpty(0)
                .flatMap(attempt -> {
                    int newAttempt = attempt + 1;
                    if (newAttempt >= 3) {
                        System.err.println("Máximo de intentos alcanzado para el pedido: " + orderMessage.getOrderId());
                        return Mono.empty();
                    } else {
                        return redisFailureService.storeFailedMessage(orderMessage.getOrderId(), originalMessage,
                                newAttempt);
                    }
                }).subscribe();
    }
}
