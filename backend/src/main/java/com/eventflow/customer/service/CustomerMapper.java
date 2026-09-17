package com.eventflow.customer.service;

import com.eventflow.customer.dto.CustomerResponse;
import com.eventflow.customer.entity.Customer;

public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.id,
                customer.name,
                customer.email,
                customer.createdAt,
                customer.updatedAt
        );
    }
}
