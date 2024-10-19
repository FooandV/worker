package com.foo.worker.service;

import com.foo.worker.models.CustomerDetails;
import com.foo.worker.models.Order;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.models.ProductDetails;

import reactor.core.publisher.Mono;

/*
 * Esta clase es responsable de procesar el pedido después de recibir el mensaje 
 * y enriquecerlo con datos adicionales
 */
public interface OrderProcessorService {

    Mono<Order> processOrder(OrderMessage orderMessage);
    Order createEnrichedOrder(OrderMessage orderMessage, CustomerDetails customer, ProductDetails product);

}
