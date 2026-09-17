package com.eventflow.product.resource;

import com.eventflow.common.IdValidator;
import com.eventflow.common.PageResponse;
import com.eventflow.common.PaginationParams;
import com.eventflow.product.dto.CreateProductRequest;
import com.eventflow.product.dto.ProductResponse;
import com.eventflow.product.service.ProductService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.Set;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    private static final Set<String> LIST_PARAMS = Set.of("page", "size", "active");

    @Inject
    ProductService productService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(CreateProductRequest request) {
        ProductResponse response = productService.create(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    public PageResponse<ProductResponse> list(@Context UriInfo uriInfo) {
        PaginationParams pagination = PaginationParams.parse(uriInfo.getQueryParameters(), LIST_PARAMS);
        Boolean active = ProductService.parseActiveFilter(uriInfo.getQueryParameters());
        return productService.list(pagination, active);
    }

    @GET
    @Path("/{id}")
    public ProductResponse get(@PathParam("id") String id) {
        return productService.getById(IdValidator.parsePathId(id));
    }
}
