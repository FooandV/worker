package com.foo.worker.service;

import com.foo.worker.models.CustomerDetails;
import com.foo.worker.models.OrderMessage;
import com.foo.worker.models.ProductDetails;

import reactor.core.publisher.Mono;

/**
 * EnrichmentService: Interface that defines methods to enrich order data,
 * including both customer and product information.
 * 
 * It is worth mentioning that Resilience4j is used for retries, and Reactor is
 * used when finer-grained reactive control is needed.
 * 
 * @author Freyder Otalvaro
 * @version 1.0
 * @since 2024-10-17
 */
public interface EnrichmentService {

    Mono<CustomerDetails> enrichCustomerWithResilience(OrderMessage orderMessage);
    
    Mono<CustomerDetails> enrichCustomerWithReactor(OrderMessage orderMessage);
    
    Mono<ProductDetails> enrichProductWithResilience(OrderMessage orderMessage);
}
