package com.eventflow.common.exception;

import com.eventflow.common.BusinessException;
import com.eventflow.common.ErrorResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {

    @Override
    public Response toResponse(BusinessException exception) {
        ErrorResponse body = new ErrorResponse(
                exception.getStatus(),
                exception.getErrorCode().name(),
                exception.getMessage(),
                exception.getDetails()
        );
        return Response.status(exception.getStatus()).entity(body).build();
    }
}
