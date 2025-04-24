package com.foo.worker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProductDetails: This class represents detailed product information retrieved 
 * from an external Go-based API. It includes key data about the product such as 
 * its identifier, name, description, and price.
 * 
 * It is used to enrich incoming orders from Kafka with detailed information
 * about the products included in the order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetails {
    private String productId;
    private String name;
    private String description;
    private Double price;
}
