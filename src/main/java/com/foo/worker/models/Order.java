package com.foo.worker.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

import lombok.Data;

/**
 * The Order class represents a processed order that will be stored in MongoDB.
 * This class is used after the order data has been enriched with detailed
 * customer and product information retrieved from external Go-based APIs.
 */
@Data
@Document(collection = "orders")
public class Order {

    @Id
    private String id;
    private String orderId;
    private String customerId;
    private List<Product> products;
}
