package com.eventflow.common.exception;

import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class NotAllowedExceptionMapper implements ExceptionMapper<NotAllowedException> {

    @Override
    public Response toResponse(NotAllowedException exception) {
        ErrorResponse body = new ErrorResponse(
                405,
                ErrorCode.METHOD_NOT_ALLOWED.name(),
                "Method not allowed",
                List.of()
        );
        return Response.status(405)
                .entity(body)
                .header("Allow", exception.getResponse().getHeaderString("Allow"))
                .build();
    }
}
