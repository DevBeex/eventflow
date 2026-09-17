package com.eventflow.customer.resource;

import com.eventflow.common.PageResponse;
import com.eventflow.common.PaginationParams;
import com.eventflow.customer.dto.CreateCustomerRequest;
import com.eventflow.customer.dto.CustomerResponse;
import com.eventflow.customer.service.CustomerService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.Set;

@Path("/api/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    private static final Set<String> LIST_PARAMS = Set.of("page", "size");

    @Inject
    CustomerService customerService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(CreateCustomerRequest request) {
        CustomerResponse response = customerService.create(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    public PageResponse<CustomerResponse> list(@Context UriInfo uriInfo) {
        PaginationParams pagination = PaginationParams.parse(uriInfo.getQueryParameters(), LIST_PARAMS);
        return customerService.list(pagination);
    }
}
