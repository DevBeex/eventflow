package com.eventflow.common.exception;

import com.eventflow.common.ErrorCode;
import com.eventflow.common.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Comparator;
import java.util.List;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ErrorResponse.FieldError> details = exception.getConstraintViolations().stream()
                .sorted(Comparator.comparing(v -> fieldName(v)))
                .map(v -> new ErrorResponse.FieldError(fieldName(v), v.getMessage()))
                .toList();

        ErrorResponse body = new ErrorResponse(
                400,
                ErrorCode.VALIDATION_ERROR.name(),
                "Invalid request",
                details
        );
        return Response.status(400).entity(body).build();
    }

    private String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        if (path.startsWith("create") || path.startsWith("update") || path.startsWith("arg")) {
            int dot = path.indexOf('.');
            return dot >= 0 ? path.substring(dot + 1) : path;
        }
        return path;
    }
}
