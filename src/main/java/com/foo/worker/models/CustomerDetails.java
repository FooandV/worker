package com.foo.worker.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * CustomerDetails: Represents the details of a customer.
 * This class maps the Customer object received from the external Go API.
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
