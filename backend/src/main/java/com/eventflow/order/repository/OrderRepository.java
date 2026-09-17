package com.eventflow.order.repository;

import com.eventflow.order.entity.Order;
import com.eventflow.order.entity.OrderStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    public long countFiltered(OrderStatus status, Long customerId) {
        return find(buildEntityQuery(status, customerId), buildParams(status, customerId)).count();
    }

    public List<Order> findPage(int page, int size, OrderStatus status, Long customerId) {
        return find("SELECT o FROM Order o JOIN FETCH o.customer WHERE " + buildAliasQuery(status, customerId)
                + " ORDER BY o.createdAt DESC, o.id DESC", buildParams(status, customerId))
                .page(page, size)
                .list();
    }

    public Optional<Order> findByIdForUpdate(long id) {
        return find("id = ?1", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }

    public Optional<Order> findByIdWithCustomer(long id) {
        return find("SELECT o FROM Order o JOIN FETCH o.customer WHERE o.id = ?1", id).firstResultOptional();
    }

    private String buildEntityQuery(OrderStatus status, Long customerId) {
        StringBuilder sb = new StringBuilder("1=1");
        if (status != null) {
            sb.append(" AND status = :status");
        }
        if (customerId != null) {
            sb.append(" AND customer.id = :customerId");
        }
        return sb.toString();
    }

    private String buildAliasQuery(OrderStatus status, Long customerId) {
        StringBuilder sb = new StringBuilder("1=1");
        if (status != null) {
            sb.append(" AND o.status = :status");
        }
        if (customerId != null) {
            sb.append(" AND o.customer.id = :customerId");
        }
        return sb.toString();
    }

    private Map<String, Object> buildParams(OrderStatus status, Long customerId) {
        Map<String, Object> params = new HashMap<>();
        if (status != null) {
            params.put("status", status);
        }
        if (customerId != null) {
            params.put("customerId", customerId);
        }
        return params;
    }
}
