package com.eventflow.common;

import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        List<FieldError> details
) {
    public record FieldError(String field, String message) {
    }
}
