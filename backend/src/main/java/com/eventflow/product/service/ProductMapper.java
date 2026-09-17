package com.eventflow.product.service;

import com.eventflow.common.MoneyUtils;
import com.eventflow.product.dto.ProductResponse;
import com.eventflow.product.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id,
                product.name,
                product.description,
                product.price,
                MoneyUtils.CURRENCY,
                product.active,
                product.createdAt,
                product.updatedAt
        );
    }
}
