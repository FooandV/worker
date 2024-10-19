package com.foo.worker.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProductDetails: Esta clase representa los detalles de un producto, obtenidos de la API externa
 * desarrollada en Go. Los datos incluyen información clave sobre el producto,
 * como su identificador, nombre, descripción y precio.
 * Se utiliza para enriquecer los pedidos que llegan desde Kafka con información
 * detallada sobre los productos involucrados en el pedido.
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