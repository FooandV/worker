package com.foo.worker.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.foo.worker.models.CustomerDetails;
import com.foo.worker.models.Order;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.models.ProductDetails;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for OrderProcessorServiceImpl using Mockito and StepVerifier.
 * 
 * These tests validate different scenarios:
 * - Successful order processing
 * - Inactive customer
 * - Product not found
 * - Lock not acquired
 * 
 * Author: Freyder Otalvaro
 */
public class OrderProcessorServiceImplTest {

    @Mock
    private EnrichmentService enrichmentService;

    @Mock
    private OrderStorageService orderStorageService;

    @Mock
    private RedisLockService redisLockService;

    @InjectMocks
    private OrderProcessorServiceImpl orderProcessorService;

    private OrderMessage orderMessage;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a test order
        orderMessage = new OrderMessage();
        orderMessage.setOrderId("order-123");
        orderMessage.setCustomerId("customer-456");

        // Test data
        CustomerDetails customerDetails = new CustomerDetails("customer-456", "John Doe", "john.doe@example.com", true);
        ProductDetails productDetails = new ProductDetails("product-789", "Laptop", "High-performance laptop", 999.99);

        // Mock behavior
        when(enrichmentService.enrichCustomerWithResilience(any(OrderMessage.class))).thenReturn(Mono.just(customerDetails));
        when(enrichmentService.enrichProductWithResilience(any(OrderMessage.class))).thenReturn(Mono.just(productDetails));
        when(redisLockService.acquireLock(any(String.class))).thenReturn(Mono.just(true));

        // Simulate saving in MongoDB
        Order order = new Order();
        order.setOrderId("order-123");
        when(orderStorageService.saveOrder(any(Order.class))).thenReturn(Mono.just(order));
    }

    @Test
    public void testProcessOrder_Successful() {
        // Verify that the order is successfully processed and stored
        StepVerifier.create(orderProcessorService.processOrder(orderMessage))
                .expectNextMatches(order -> order.getOrderId().equals("order-123"))
                .verifyComplete();
    }

    @Test
    public void testProcessOrder_CustomerInactive() {
        // Simulate an inactive customer
        CustomerDetails inactiveCustomer = new CustomerDetails("customer-456", "John Doe", "john.doe@example.com", false);
        when(enrichmentService.enrichCustomerWithResilience(any(OrderMessage.class))).thenReturn(Mono.just(inactiveCustomer));

        // Expect failure due to inactive customer
        StepVerifier.create(orderProcessorService.processOrder(orderMessage))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Inactive customer"))
                .verify();
    }

    @Test
    public void testProcessOrder_ProductNotFound() {
        // Simulate product not found
        when(enrichmentService.enrichProductWithResilience(any(OrderMessage.class))).thenReturn(Mono.error(new RuntimeException("Product not found")));

        // Expect failure due to missing product
        StepVerifier.create(orderProcessorService.processOrder(orderMessage))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Product not found"))
                .verify();
    }

    @Test
    public void testProcessOrder_LockNotAcquired() {
        // Simulate lock not acquired
        when(redisLockService.acquireLock(any(String.class))).thenReturn(Mono.just(false));

        // Expect failure due to lock acquisition failure
        StepVerifier.create(orderProcessorService.processOrder(orderMessage))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Order is already being processed"))
                .verify();
    }
}
