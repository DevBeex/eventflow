package com.eventflow.product.service;

import com.eventflow.common.BusinessException;
import com.eventflow.common.ClockService;
import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import com.eventflow.common.MoneyUtils;
import com.eventflow.common.PageResponse;
import com.eventflow.common.PaginationParams;
import com.eventflow.common.TextUtils;
import com.eventflow.common.ValidationException;
import com.eventflow.product.dto.CreateProductRequest;
import com.eventflow.product.dto.ProductResponse;
import com.eventflow.product.entity.Product;
import com.eventflow.product.repository.ProductRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ProductService {

    @Inject
    ProductRepository productRepository;

    @Inject
    ClockService clockService;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (request == null) {
            throw new ValidationException("Invalid request",
                    List.of(new ErrorResponse.FieldError("body", "request body is required")));
        }

        List<ErrorResponse.FieldError> errors = new ArrayList<>();

        String name = request.getName() == null ? null : TextUtils.normalizeName(request.getName());
        if (name == null || name.isEmpty() || name.length() > 120) {
            errors.add(new ErrorResponse.FieldError("name", "must be between 1 and 120 characters"));
        }

        String description = request.getDescription() == null
                ? null
                : TextUtils.normalizeDescription(request.getDescription());
        if (description != null && description.length() > 1000) {
            errors.add(new ErrorResponse.FieldError("description", "must be at most 1000 characters"));
        }

        BigDecimal price = null;
        try {
            price = MoneyUtils.validateAndNormalizePrice(request.getPrice(), "price");
        } catch (ValidationException ex) {
            errors.addAll(ex.getDetails());
        }

        boolean active;
        if (!request.isActiveProvided()) {
            active = true;
        } else if (request.getActive() == null) {
            errors.add(new ErrorResponse.FieldError("active", "must not be null"));
            active = true;
        } else {
            active = request.getActive();
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid request", errors);
        }

        var now = clockService.now();
        Product product = new Product();
        product.name = name;
        product.description = description;
        product.price = price;
        product.active = active;
        product.createdAt = now;
        product.updatedAt = now;
        productRepository.persist(product);
        return ProductMapper.toResponse(product);
    }

    public PageResponse<ProductResponse> list(PaginationParams pagination, Boolean active) {
        long total = productRepository.countFiltered(active);
        List<ProductResponse> items = productRepository.findPage(pagination.page(), pagination.size(), active)
                .stream()
                .map(ProductMapper::toResponse)
                .toList();
        return PageResponse.of(items, pagination.page(), pagination.size(), total);
    }

    public ProductResponse getById(long id) {
        Product product = productRepository.findById(id);
        if (product == null) {
            throw new BusinessException(404, ErrorCode.PRODUCT_NOT_FOUND, "Product not found");
        }
        return ProductMapper.toResponse(product);
    }

    public static Boolean parseActiveFilter(Map<String, List<String>> queryParams) {
        List<String> values = queryParams.get("active");
        if (values == null || values.isEmpty()) {
            return null;
        }
        String value = values.get(0);
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw new ValidationException("Invalid request",
                List.of(new ErrorResponse.FieldError("active", "must be true or false")));
    }
}
