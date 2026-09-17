package com.eventflow.order.service;

import com.eventflow.common.BusinessException;
import com.eventflow.common.ClockService;
import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import com.eventflow.common.MoneyUtils;
import com.eventflow.common.PageResponse;
import com.eventflow.common.PaginationParams;
import com.eventflow.common.ValidationException;
import com.eventflow.customer.entity.Customer;
import com.eventflow.customer.repository.CustomerRepository;
import com.eventflow.event.OrderCreatedEvent;
import com.eventflow.order.dto.CreateOrderRequest;
import com.eventflow.order.dto.OrderDetailResponse;
import com.eventflow.order.dto.OrderSummaryResponse;
import com.eventflow.order.entity.Order;
import com.eventflow.order.entity.OrderItem;
import com.eventflow.order.entity.OrderStatus;
import com.eventflow.order.repository.OrderItemRepository;
import com.eventflow.order.repository.OrderRepository;
import com.eventflow.outbox.entity.OutboxEvent;
import com.eventflow.outbox.entity.OutboxStatus;
import com.eventflow.outbox.repository.OutboxRepository;
import com.eventflow.product.entity.Product;
import com.eventflow.product.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Inject
    OrderRepository orderRepository;

    @Inject
    OrderItemRepository orderItemRepository;

    @Inject
    CustomerRepository customerRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    OutboxRepository outboxRepository;

    @Inject
    ClockService clockService;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public OrderDetailResponse create(CreateOrderRequest request) {
        validateCreateStructure(request);

        Customer customer = customerRepository.findById(request.customerId());
        if (customer == null) {
            throw new BusinessException(404, ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found");
        }

        List<CreateOrderRequest.OrderItemRequest> sortedItems = request.items().stream()
                .sorted(Comparator.comparing(CreateOrderRequest.OrderItemRequest::productId))
                .toList();

        List<ResolvedItem> resolvedItems = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest item : sortedItems) {
            Product product = productRepository.findById(item.productId());
            if (product == null) {
                throw new BusinessException(404, ErrorCode.PRODUCT_NOT_FOUND, "Product not found");
            }
            if (!product.active) {
                throw new BusinessException(409, ErrorCode.PRODUCT_INACTIVE, "Product is not active");
            }
            BigDecimal subtotal = MoneyUtils.multiply(product.price, item.quantity());
            resolvedItems.add(new ResolvedItem(product, item.quantity(), product.price, subtotal));
        }

        BigDecimal total = resolvedItems.stream()
                .map(ResolvedItem::subtotal)
                .reduce(java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.UNNECESSARY), MoneyUtils::add);

        if (total.compareTo(MoneyUtils.MIN_TOTAL) < 0 || total.compareTo(MoneyUtils.MAX_TOTAL) > 0) {
            throw new ValidationException("Invalid request",
                    List.of(new ErrorResponse.FieldError("items", "order total is out of allowed range")));
        }

        var now = clockService.now();
        UUID eventId = UUID.randomUUID();

        Order order = new Order();
        order.customer = customer;
        order.status = OrderStatus.CREATED;
        order.total = total;
        order.createdAt = now;
        order.updatedAt = now;
        orderRepository.persist(order);

        List<OrderItem> persistedItems = new ArrayList<>();
        for (ResolvedItem resolved : resolvedItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.order = order;
            orderItem.product = resolved.product();
            orderItem.quantity = resolved.quantity();
            orderItem.unitPrice = resolved.unitPrice();
            orderItem.subtotal = resolved.subtotal();
            orderItemRepository.persist(orderItem);
            persistedItems.add(orderItem);
        }

        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                OrderCreatedEvent.TYPE,
                OrderCreatedEvent.SCHEMA_VERSION,
                order.id,
                customer.id,
                total,
                MoneyUtils.CURRENCY,
                now
        );

        OutboxEvent outbox = new OutboxEvent();
        outbox.eventId = eventId;
        outbox.orderId = order.id;
        outbox.eventType = OrderCreatedEvent.TYPE;
        outbox.payload = serializePayload(event);
        outbox.status = OutboxStatus.PENDING;
        outbox.attempts = 0;
        outbox.nextAttemptAt = now;
        outbox.createdAt = now;
        outboxRepository.persist(outbox);

        LOG.infof("Order %d created with pending event %s", order.id, eventId);
        return OrderMapper.toDetail(order, persistedItems);
    }

    public PageResponse<OrderSummaryResponse> list(PaginationParams pagination, OrderStatus status, Long customerId) {
        long total = orderRepository.countFiltered(status, customerId);
        List<OrderSummaryResponse> items = orderRepository.findPage(pagination.page(), pagination.size(), status, customerId)
                .stream()
                .map(OrderMapper::toSummary)
                .toList();
        return PageResponse.of(items, pagination.page(), pagination.size(), total);
    }

    public OrderDetailResponse getById(long id) {
        Order order = orderRepository.findByIdWithCustomer(id).orElse(null);
        if (order == null) {
            throw new BusinessException(404, ErrorCode.ORDER_NOT_FOUND, "Order not found");
        }
        List<OrderItem> items = orderItemRepository.findByOrderIdWithProduct(id);
        return OrderMapper.toDetail(order, items);
    }

    @Transactional
    public OrderSummaryResponse updateStatus(long id, String requestedStatusRaw) {
        if (requestedStatusRaw == null || requestedStatusRaw.isBlank()) {
            throw new ValidationException("Invalid request",
                    List.of(new ErrorResponse.FieldError("status", "must not be blank")));
        }

        OrderStatus requestedStatus;
        try {
            requestedStatus = OrderStatus.valueOf(requestedStatusRaw);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid request",
                    List.of(new ErrorResponse.FieldError("status", "must be CREATED, COMPLETED or CANCELLED")));
        }

        Order order = orderRepository.findByIdForUpdate(id).orElse(null);
        if (order == null) {
            throw new BusinessException(404, ErrorCode.ORDER_NOT_FOUND, "Order not found");
        }

        OrderStatus current = order.status;
        if (current == requestedStatus) {
            return OrderMapper.toSummary(order);
        }

        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new BusinessException(409, ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                    "Invalid order status transition");
        }

        if (current == OrderStatus.CREATED
                && (requestedStatus == OrderStatus.COMPLETED || requestedStatus == OrderStatus.CANCELLED)) {
            order.status = requestedStatus;
            order.updatedAt = clockService.now();
            return OrderMapper.toSummary(order);
        }

        throw new BusinessException(409, ErrorCode.INVALID_ORDER_STATUS_TRANSITION,
                "Invalid order status transition");
    }

    public static OrderStatus parseStatusFilter(Map<String, List<String>> queryParams) {
        List<String> values = queryParams.get("status");
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(values.get(0));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid request",
                    List.of(new ErrorResponse.FieldError("status", "must be CREATED, COMPLETED or CANCELLED")));
        }
    }

    private void validateCreateStructure(CreateOrderRequest request) {
        List<ErrorResponse.FieldError> errors = new ArrayList<>();
        if (request == null) {
            throw new ValidationException("Invalid request",
                    List.of(new ErrorResponse.FieldError("body", "request body is required")));
        }

        if (request.customerId() == null) {
            errors.add(new ErrorResponse.FieldError("customerId", "must not be null"));
        } else if (request.customerId() <= 0) {
            errors.add(new ErrorResponse.FieldError("customerId", "must be a positive integer"));
        }

        if (request.items() == null) {
            errors.add(new ErrorResponse.FieldError("items", "must not be null"));
        } else if (request.items().isEmpty() || request.items().size() > 100) {
            errors.add(new ErrorResponse.FieldError("items", "must contain between 1 and 100 elements"));
        } else {
            Set<Long> seen = new HashSet<>();
            for (int i = 0; i < request.items().size(); i++) {
                CreateOrderRequest.OrderItemRequest item = request.items().get(i);
                String prefix = "items[" + i + "]";
                if (item == null) {
                    errors.add(new ErrorResponse.FieldError(prefix, "must not be null"));
                    continue;
                }
                if (item.productId() == null) {
                    errors.add(new ErrorResponse.FieldError(prefix + ".productId", "must not be null"));
                } else if (item.productId() <= 0) {
                    errors.add(new ErrorResponse.FieldError(prefix + ".productId", "must be a positive integer"));
                } else if (!seen.add(item.productId())) {
                    errors.add(new ErrorResponse.FieldError(prefix + ".productId", "must be unique within the order"));
                }
                if (item.quantity() == null) {
                    errors.add(new ErrorResponse.FieldError(prefix + ".quantity", "must not be null"));
                } else if (item.quantity() < 1 || item.quantity() > 999) {
                    errors.add(new ErrorResponse.FieldError(prefix + ".quantity", "must be an integer between 1 and 999"));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid request", errors);
        }
    }

    private String serializePayload(OrderCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private record ResolvedItem(Product product, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}
