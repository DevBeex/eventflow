package com.eventflow.customer.repository;

import com.eventflow.customer.entity.Customer;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class CustomerRepository implements PanacheRepository<Customer> {

    public long countAll() {
        return count();
    }

    public List<Customer> findPage(int page, int size) {
        return find("ORDER BY createdAt DESC, id DESC")
                .page(page, size)
                .list();
    }
}
