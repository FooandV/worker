package com.foo.worker.service;

import org.springframework.stereotype.Service;

import com.foo.worker.models.CustomerDetails;
import com.foo.worker.models.Order;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.models.ProductDetails;

import reactor.core.publisher.Mono;

/**
 * OrderProcessorServiceImpl: Processes incoming order messages received from Kafka.
 *
 * Main functionality:
 * - Enriches customer and product data through external Go-based APIs.
 * - Persists enriched orders in MongoDB.
 * - Manages concurrency using Redis locks to prevent duplicate processing.
 *
 * Error Handling:
 * - Applies automatic retries using Resilience4j for API failures.
 * - Ensures single processing of orders using distributed locks in Redis.
 *
 * Dependencies:
 * - EnrichmentService: Handles customer and product data enrichment.
 * - OrderStorageService: Persists orders in MongoDB.
 * - RedisLockService: Manages distributed locks with Redis.
 * 
 * @author Freyder Otalvaro
 * @version 1.2
 * @since 2024-10-19
 */
@Service
public class OrderProcessorServiceImpl implements OrderProcessorService {

    private final EnrichmentService enrichmentService;
    private final OrderStorageService orderStorageService;
    private final RedisLockService redisLockService;

    /**
     * Constructor that initializes the required services for order processing.
     */
    public OrderProcessorServiceImpl(EnrichmentService enrichmentService,
                                     OrderStorageService orderStorageService,
                                     RedisLockService redisLockService) {
        this.enrichmentService = enrichmentService;
        this.orderStorageService = orderStorageService;
        this.redisLockService = redisLockService;
    }

    /**
     * Processes an incoming order message received from Kafka.
     * Enriches customer and product data, validates them, and stores the order in MongoDB.
     *
     * @param orderMessage The message containing basic order data from Kafka.
     * @return Mono<Order> A reactive stream representing the result of the processing.
     * @throws RuntimeException if the customer is inactive or product is not found.
     */
    @Override
    public Mono<Order> processOrder(OrderMessage orderMessage) {
        // Try to acquire a lock before processing the order
        return redisLockService.acquireLock(orderMessage.getOrderId())
                .flatMap(acquired -> {
                    if (!acquired) {
                        // If the lock could not be acquired, stop processing
                        return Mono.error(new RuntimeException("Order is already being processed"));
                    }

                    // If the lock is acquired, proceed with enrichment
                    return enrichmentService.enrichCustomerWithResilience(orderMessage)
                            .zipWith(enrichmentService.enrichProductWithResilience(orderMessage))
                            .flatMap(tuple -> {
                                CustomerDetails customer = tuple.getT1();
                                ProductDetails product = tuple.getT2();

                                if (!customer.getActive()) {
                                    return Mono.error(new RuntimeException("Inactive customer"));
                                }

                                if (product == null || product.getProductId() == null) {
                                    return Mono.error(new RuntimeException("Product not found"));
                                }

                                System.out.println("Enriched data: Customer: " + customer + ", Product: " + product);
                                Order order = createEnrichedOrder(orderMessage, customer, product);
                                return orderStorageService.saveOrder(order);
                            })
                            .doFinally(signalType -> {
                                redisLockService.releaseLock(orderMessage.getOrderId())
                                    .defaultIfEmpty(false)
                                    .subscribe(success -> {
                                        if (!success) {
                                            System.out.println("Failed to release lock for order: " + orderMessage.getOrderId());
                                        } else {
                                            System.out.println("Lock released for order: " + orderMessage.getOrderId());
                                        }
                                    }, error -> {
                                        System.out.println("Error releasing lock: " + error.getMessage());
                                    });
                            });
                });
    }

    /**
     * Creates an enriched Order object using data from Kafka and the enrichment APIs.
     *
     * @param orderMessage The message containing basic order information.
     * @param customer     The enriched customer details.
     * @param product      The enriched product details.
     * @return Order The fully enriched order object ready for persistence.
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
