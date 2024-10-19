package com.foo.worker.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

import lombok.Data;

/**
 * La clase Order representa un pedido procesado que será almacenado en MongoDB.
 * Esta clase se utiliza después de que los datos del pedido han sido
 * enriquecidos con información detallada del cliente y de los productos
 * obtenidos a través de las APIs externas desarrolladas en Go.
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
