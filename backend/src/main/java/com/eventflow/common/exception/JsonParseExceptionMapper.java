package com.eventflow.common.exception;

import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import com.fasterxml.jackson.core.JsonParseException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    @Override
    public Response toResponse(JsonParseException exception) {
        ErrorResponse body = new ErrorResponse(
                400,
                ErrorCode.VALIDATION_ERROR.name(),
                "Invalid request",
                List.of(new ErrorResponse.FieldError("body", "invalid JSON"))
        );
        return Response.status(400).entity(body).build();
    }
}
