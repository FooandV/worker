package com.foo.worker.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * CustomerDetails: Representa los detalles de un cliente
 * Clase que mapea el Customer de la API que viene de GO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDetails {
    private String customerId;
    private String name;
    private String email;
    private Boolean active;
}