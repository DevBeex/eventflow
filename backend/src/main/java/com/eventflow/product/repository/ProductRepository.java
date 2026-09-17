package com.eventflow.product.repository;

import com.eventflow.product.entity.Product;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public long countFiltered(Boolean active) {
        if (active == null) {
            return count();
        }
        return count("active = ?1", active);
    }

    public List<Product> findPage(int page, int size, Boolean active) {
        if (active == null) {
            return find("ORDER BY createdAt DESC, id DESC")
                    .page(page, size)
                    .list();
        }
        return find("active = ?1 ORDER BY createdAt DESC, id DESC", active)
                .page(page, size)
                .list();
    }

    public List<Product> findByIdsSorted(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, Object> params = new HashMap<>();
        params.put("ids", ids);
        return find("id in :ids", params).list();
    }
}
