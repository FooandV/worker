package com.foo.worker.models;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * OrderMessage: Esta clase representa el mensaje que se recibe desde Kafka.
 * Contiene información básica del pedido, como el ID del pedido "orderId", el ID del cliente "customerId", 
 * y una lista de productos asociados al pedido-orden "order". 
 * Es utilizada para la transmisión y procesamiento de pedidos en el sistema.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMessage {

    private String orderId;
    private String customerId;
    private List<Product> products;
}
