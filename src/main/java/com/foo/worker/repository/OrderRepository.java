package com.foo.worker.repository;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.foo.worker.models.Order;


public interface OrderRepository extends ReactiveMongoRepository<Order, String>  {

}
