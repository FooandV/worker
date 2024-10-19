package com.foo.worker.service;

import com.foo.worker.models.CustomerDetails;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.models.ProductDetails;

import reactor.core.publisher.Mono;

/**
 * EnrichmentService: Interfaz que define los métodos para enriquecer los datos de un pedido.
 * tanto cliente "Customer" como los datos del producto"Product"
 * cabe destacar que se utilizo Resilience4J para reintentos y Reactor si se quiere una inspeccion mas granular
 * @author Freyder Otalvaro
 * @version 1.0
 * @since 2024-10-17
 */
public interface EnrichmentService {
    
    Mono<CustomerDetails> enrichCustomerWithResilience(OrderMessage orderMessage);
    Mono<CustomerDetails> enrichCustomerWithReactor(OrderMessage orderMessage);
    
    Mono<ProductDetails> enrichProductWithResilience(OrderMessage orderMessage);
}
