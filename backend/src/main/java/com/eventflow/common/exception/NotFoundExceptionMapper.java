package com.eventflow.common.exception;

import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        ErrorResponse body = new ErrorResponse(
                404,
                ErrorCode.RESOURCE_NOT_FOUND.name(),
                "Resource not found",
                List.of()
        );
        return Response.status(404).entity(body).build();
    }
}
