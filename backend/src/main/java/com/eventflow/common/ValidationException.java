package com.eventflow.common;

import java.util.List;

public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        this(message, List.of());
    }

    public ValidationException(String message, List<ErrorResponse.FieldError> details) {
        super(400, ErrorCode.VALIDATION_ERROR, message, details);
    }
}
