package com.foo.worker.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;

import com.foo.worker.models.Order;

/**
 * Configuration class responsible for creating MongoDB indexes
 * on the "orders" collection when the application is ready.
 * 
 * This improves query performance on frequently accessed fields
 * such as "orderId" and "customerId".
 */
@Configuration
public class MongoIndexConfig {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        ReactiveIndexOperations indexOps = reactiveMongoTemplate.indexOps(Order.class);

        // Create an index on the "orderId" field of the "orders" collection
        indexOps.ensureIndex(new Index().on("orderId", Sort.Direction.ASC))
                .doOnSuccess(success -> System.out.println("Index created for orderId."))
                .doOnError(error -> System.err.println("Error creating index for orderId: " + error.getMessage()))
                .subscribe();

        // Create an index on the "customerId" field of the "orders" collection
        indexOps.ensureIndex(new Index().on("customerId", Sort.Direction.ASC))
                .doOnSuccess(success -> System.out.println("Index created for customerId."))
                .doOnError(error -> System.err.println("Error creating index for customerId: " + error.getMessage()))
                .subscribe();

        System.out.println("Indexes created in MongoDB.");
    }

}
