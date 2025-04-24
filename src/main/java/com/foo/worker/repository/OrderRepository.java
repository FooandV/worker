package com.foo.worker.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.foo.worker.models.Order;

/**
 * OrderRepository: Reactive repository interface for managing Order documents
 * in MongoDB. It extends ReactiveMongoRepository to provide non-blocking
 * CRUD operations on the "orders" collection.
 */
public interface OrderRepository extends ReactiveMongoRepository<Order, String> {

}
