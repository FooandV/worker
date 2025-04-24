package com.foo.worker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Product: Represents a product included in an order.
 * This class is used both in the incoming Kafka message (OrderMessage)
 * and in the enriched order stored in MongoDB.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private String productId;
    private String name;
    private Double price;
}
