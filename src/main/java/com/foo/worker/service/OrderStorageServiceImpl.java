package com.foo.worker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.foo.worker.models.Order;
import com.foo.worker.repository.OrderRepository;

import reactor.core.publisher.Mono;

/**
 * OrderStorageServiceImpl: Service implementation responsible for storing 
 * enriched orders into MongoDB using a reactive repository.
 */
@Service
public class OrderStorageServiceImpl implements OrderStorageService {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Saves the enriched order into MongoDB.
     *
     * @param order The enriched order to be persisted.
     * @return Mono<Order> representing the saved order.
     */
    @Override
    public Mono<Order> saveOrder(Order order) {
        return orderRepository.save(order);
    }
}
