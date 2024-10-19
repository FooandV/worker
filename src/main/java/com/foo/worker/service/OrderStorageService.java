package com.foo.worker.service;

import com.foo.worker.models.Order;

import reactor.core.publisher.Mono;

public interface OrderStorageService {
    Mono<Order> saveOrder(Order order);

}
