package com.foo.worker.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.foo.worker.models.Order;
import com.foo.worker.repository.OrderRepository;

import reactor.core.publisher.Mono;

/*
 *  guardar el pedido enriquecido en MongoDB
 */
@Service
public class OrderStorageServiceImpl implements OrderStorageService  {

    @Autowired
    private OrderRepository orderRepository;


    // Guardar el pedido enriquecido en MongoDB
    public Mono<Order> saveOrder(Order order) {
        return orderRepository.save(order);
    }
}
