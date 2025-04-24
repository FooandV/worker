package com.foo.worker.service;

import com.foo.worker.models.Order;

import reactor.core.publisher.Mono;

/**
 * OrderStorageService: Interface responsible for persisting enriched Order objects
 * into MongoDB using a reactive approach.
 */
public interface OrderStorageService {
    Mono<Order> saveOrder(Order order);
}
