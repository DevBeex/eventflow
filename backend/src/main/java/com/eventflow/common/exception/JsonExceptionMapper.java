package com.eventflow.common.exception;

import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class JsonExceptionMapper implements ExceptionMapper<MismatchedInputException> {

    @Override
    public Response toResponse(MismatchedInputException exception) {
        String field = "body";
        String message = "invalid JSON";

        if (exception instanceof UnrecognizedPropertyException upe) {
            field = upe.getPropertyName();
            message = "unknown property";
        } else if (exception.getPath() != null && !exception.getPath().isEmpty()) {
            field = exception.getPath().get(0).getFieldName();
            message = "invalid type or value";
        }

        ErrorResponse body = new ErrorResponse(
                400,
                ErrorCode.VALIDATION_ERROR.name(),
                "Invalid request",
                List.of(new ErrorResponse.FieldError(field, message))
        );
        return Response.status(400).entity(body).build();
    }
}
