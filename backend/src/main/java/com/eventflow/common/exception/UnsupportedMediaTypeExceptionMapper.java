package com.eventflow.common.exception;

import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class UnsupportedMediaTypeExceptionMapper implements ExceptionMapper<NotSupportedException> {

    @Override
    public Response toResponse(NotSupportedException exception) {
        ErrorResponse body = new ErrorResponse(
                415,
                ErrorCode.UNSUPPORTED_MEDIA_TYPE.name(),
                "Unsupported media type",
                List.of()
        );
        return Response.status(415).entity(body).build();
    }
}
