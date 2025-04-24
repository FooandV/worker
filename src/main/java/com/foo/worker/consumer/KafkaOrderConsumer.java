package com.foo.worker.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.service.OrderProcessorService;
import com.foo.worker.service.RedisFailureService;

import reactor.core.publisher.Mono;

/**
 * KafkaOrderConsumer: This class is responsible for consuming order messages 
 * from a Kafka topic. It processes the received messages, enriches them with additional 
 * customer and product data using external Go APIs, and stores the processed orders 
 * in MongoDB. If processing fails, it handles retries and stores failed messages 
 * in Redis for future attempts.
 */
@Service
public class KafkaOrderConsumer {

    private final OrderProcessorService orderProcessorService;
    private final RedisFailureService redisFailureService;
    private final ObjectMapper objectMapper = new ObjectMapper(); 

    /**
     * KafkaOrderConsumer constructor.
     * @param orderProcessorService Service responsible for processing and saving orders.
     * @param redisFailureService Service responsible for managing failed messages in Redis.
     */
    public KafkaOrderConsumer(OrderProcessorService orderProcessorService, RedisFailureService redisFailureService) {
        this.orderProcessorService = orderProcessorService;
        this.redisFailureService = redisFailureService;
    }

    /**
     * Listens to messages from the "orders" Kafka topic.
     * @param message The incoming message in JSON format.
     */
    @KafkaListener(topics = "orders", groupId = "order_group")
    public void consume(String message) {
        System.out.println("Order received: " + message);
        try {
            OrderMessage orderMessage = objectMapper.readValue(message, OrderMessage.class);
            orderProcessorService.processOrder(orderMessage)
                    .subscribe(savedOrder -> {
                        System.out.println("Order stored in MongoDB with ID: " + savedOrder.getId());
                    }, error -> {
                        System.err.println("Error processing the order: " + error.getMessage());
                        // Handles failed orders and stores the message in Redis for retry
                        handleFailedOrder(orderMessage, message);
                    });
        } catch (Exception e) {
            System.err.println("Error processing the message: " + e.getMessage());
        }
    }

    /**
     * handleFailedOrder: This method handles failed order processing attempts.
     * It increments the attempt count in Redis, and if the max number of attempts is reached,
     * it stops retrying the order.
     * 
     * @param orderMessage The order that failed to process.
