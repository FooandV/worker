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

@Configuration
public class MongoIndexConfig {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        ReactiveIndexOperations indexOps = reactiveMongoTemplate.indexOps(Order.class);

        // Crear índice en el campo "orderId" de la colección en Mongo "orders"
        indexOps.ensureIndex(new Index().on("orderId", Sort.Direction.ASC))
                .doOnSuccess(success -> System.out.println("Índice creado para orderId."))
                .doOnError(error -> System.err.println("Error creando índice para orderId: " + error.getMessage()))
                .subscribe();

        // Crear índice en el campo "customerId" de la colección en Mongo "orders"
        indexOps.ensureIndex(new Index().on("customerId", Sort.Direction.ASC))
                .doOnSuccess(success -> System.out.println("Índice creado para customerId."))
                .doOnError(error -> System.err.println("Error creando índice para customerId: " + error.getMessage()))
                .subscribe();

        System.out.println("Índices creados en MongoDB.");
    }

}
