package com.eventflow.order.resource;

import com.eventflow.common.IdValidator;
import com.eventflow.common.PageResponse;
import com.eventflow.common.PaginationParams;
import com.eventflow.order.dto.CreateOrderRequest;
import com.eventflow.order.dto.OrderDetailResponse;
import com.eventflow.order.dto.OrderSummaryResponse;
import com.eventflow.order.dto.UpdateOrderStatusRequest;
import com.eventflow.order.entity.OrderStatus;
import com.eventflow.order.service.OrderService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.Set;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    private static final Set<String> LIST_PARAMS = Set.of("page", "size", "status", "customerId");

    @Inject
    OrderService orderService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(CreateOrderRequest request) {
        OrderDetailResponse response = orderService.create(request);
        return Response.status(Response.Status.CREATED)
                .entity(response)
                .location(URI.create("/api/orders/" + response.id()))
                .build();
    }

    @GET
    public PageResponse<OrderSummaryResponse> list(@Context UriInfo uriInfo) {
        PaginationParams pagination = PaginationParams.parse(uriInfo.getQueryParameters(), LIST_PARAMS);
        OrderStatus status = OrderService.parseStatusFilter(uriInfo.getQueryParameters());
        Long customerId = IdValidator.parseOptionalQueryId(uriInfo.getQueryParameters(), "customerId");
        return orderService.list(pagination, status, customerId);
    }

    @GET
    @Path("/{id}")
    public OrderDetailResponse get(@PathParam("id") String id) {
        return orderService.getById(IdValidator.parsePathId(id));
    }

    @PATCH
    @Path("/{id}/status")
    @Consumes(MediaType.APPLICATION_JSON)
    public OrderSummaryResponse updateStatus(@PathParam("id") String id, UpdateOrderStatusRequest request) {
        return orderService.updateStatus(IdValidator.parsePathId(id), request.status());
    }
}
