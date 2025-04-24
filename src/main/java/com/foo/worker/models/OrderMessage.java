package com.foo.worker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * OrderMessage: This class represents the message received from Kafka.
 * It contains basic order information such as the order ID ("orderId"),
 * customer ID ("customerId"), and a list of products associated with the order.
 * It is used for transmitting and processing orders within the system.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMessage {

    private String orderId;
    private String customerId;
    private List<Product> products;
}
