package com.foo.worker.service;

import com.foo.worker.models.CustomerDetails;
import com.foo.worker.models.Order;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.models.ProductDetails;

import reactor.core.publisher.Mono;

/**
 * OrderProcessorService: Interface responsible for processing an order 
 * after receiving the message and enriching it with additional data.
 */
public interface OrderProcessorService {

    Mono<Order> processOrder(OrderMessage orderMessage);

    Order createEnrichedOrder(OrderMessage orderMessage, CustomerDetails customer, ProductDetails product);

}
