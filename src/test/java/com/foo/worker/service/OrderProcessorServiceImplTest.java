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

        // Crear un pedido de prueba
        orderMessage = new OrderMessage();
        orderMessage.setOrderId("order-123");
        orderMessage.setCustomerId("customer-456");

        // Datos de prueba
        CustomerDetails customerDetails = new CustomerDetails("customer-456",
                                                               "John Doe", "john.doe@example.com",
                                                               true);
        ProductDetails productDetails = new ProductDetails("product-789",
                                                            "Laptop", 
                                                            "Una laptop potente", 999.99);

        // Simulaciones
        when(enrichmentService.enrichCustomerWithResilience(any(OrderMessage.class))).thenReturn(Mono.just(customerDetails));
        when(enrichmentService.enrichProductWithResilience(any(OrderMessage.class))).thenReturn(Mono.just(productDetails));
        when(redisLockService.acquireLock(any(String.class))).thenReturn(Mono.just(true));

        // Guardar en MongoDB
        Order order = new Order();
        order.setOrderId("order-123");
        when(orderStorageService.saveOrder(any(Order.class))).thenReturn(Mono.just(order));
    }

    @Test
    public void testProcessOrder_Successful() {
        // Verificar que el pedido se procese correctamente y se guarde en MongoDB
        StepVerifier.create(orderProcessorService.processOrder(orderMessage))
                .expectNextMatches(order -> order.getOrderId().equals("order-123"))
                .verifyComplete();
    }
    @Test
    public void testProcessOrder_CustomerInactive() {
        // Configurar el cliente inactivo
        CustomerDetails inactiveCustomer = new CustomerDetails("customer-456", "John Doe", "john.doe@example.com", false);
        when(enrichmentService.enrichCustomerWithResilience(any(OrderMessage.class))).thenReturn(Mono.just(inactiveCustomer));

        // Verificar que el proceso falle debido a un cliente inactivo
        StepVerifier.create(orderProcessorService.processOrder(orderMessage))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Cliente inactivo"))
                .verify();
    }
    @Test
    public void testProcessOrder_ProductNotFound() {
        // Configurar el producto como no encontrado
        when(enrichmentService.enrichProductWithResilience(any(OrderMessage.class))).thenReturn(Mono.error(new RuntimeException("Producto no encontrado")));

        // Verificar que el proceso falle debido a que no se encuentra el producto
        StepVerifier.create(orderProcessorService.processOrder(orderMessage))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Producto no encontrado"))
                .verify();
    }
    @Test
    public void testProcessOrder_LockNotAcquired() {
        // Configurar el lock como no adquirido
        when(redisLockService.acquireLock(any(String.class))).thenReturn(Mono.just(false));

        // Verificar que el proceso falle debido a que no se puede adquirir el lock
        StepVerifier.create(orderProcessorService.processOrder(orderMessage))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException && throwable.getMessage().equals("Pedido ya está siendo procesado"))
                .verify();
    }

}
