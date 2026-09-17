package com.eventflow.notification.resource;

import com.eventflow.common.IdValidator;
import com.eventflow.common.PageResponse;
import com.eventflow.common.PaginationParams;
import com.eventflow.notification.dto.NotificationResponse;
import com.eventflow.notification.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import java.util.Set;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
public class NotificationResource {

    private static final Set<String> LIST_PARAMS = Set.of("page", "size", "orderId");

    @Inject
    NotificationService notificationService;

    @GET
    public PageResponse<NotificationResponse> list(@Context UriInfo uriInfo) {
        PaginationParams pagination = PaginationParams.parse(uriInfo.getQueryParameters(), LIST_PARAMS);
        Long orderId = IdValidator.parseOptionalQueryId(uriInfo.getQueryParameters(), "orderId");
        return notificationService.list(pagination, orderId);
    }

    @GET
    @Path("/{id}")
    public NotificationResponse get(@PathParam("id") String id) {
        return notificationService.getById(IdValidator.parsePathId(id));
    }
}
