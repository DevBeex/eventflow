package com.eventflow.order.repository;

import com.eventflow.order.entity.OrderItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class OrderItemRepository implements PanacheRepository<OrderItem> {

    public List<OrderItem> findByOrderIdWithProduct(long orderId) {
        return find("""
                SELECT oi FROM OrderItem oi
                JOIN FETCH oi.product
                WHERE oi.order.id = ?1
                ORDER BY oi.product.id ASC
                """, orderId).list();
    }
}
