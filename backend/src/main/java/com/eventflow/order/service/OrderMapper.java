package com.eventflow.order.service;

import com.eventflow.common.MoneyUtils;
import com.eventflow.customer.service.CustomerMapper;
import com.eventflow.order.dto.OrderDetailResponse;
import com.eventflow.order.dto.OrderItemResponse;
import com.eventflow.order.dto.OrderSummaryResponse;
import com.eventflow.order.entity.Order;
import com.eventflow.order.entity.OrderItem;

import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(
                order.id,
                CustomerMapper.toResponse(order.customer),
                order.status.name(),
                order.total,
                MoneyUtils.CURRENCY,
                order.createdAt,
                order.updatedAt
        );
    }

    public static OrderDetailResponse toDetail(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.product.id,
                        item.product.name,
                        item.quantity,
                        item.unitPrice,
                        item.subtotal
                ))
                .toList();

        return new OrderDetailResponse(
                order.id,
                CustomerMapper.toResponse(order.customer),
                order.status.name(),
                order.total,
                MoneyUtils.CURRENCY,
                itemResponses,
                order.createdAt,
                order.updatedAt
        );
    }
}
