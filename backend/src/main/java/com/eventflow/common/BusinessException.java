package com.eventflow.common;

import java.util.List;

public class BusinessException extends RuntimeException {

    private final int status;
    private final ErrorCode errorCode;
    private final List<ErrorResponse.FieldError> details;

    public BusinessException(int status, ErrorCode errorCode, String message) {
        this(status, errorCode, message, List.of());
    }

    public BusinessException(int status, ErrorCode errorCode, String message, List<ErrorResponse.FieldError> details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public int getStatus() {
        return status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ErrorResponse.FieldError> getDetails() {
        return details;
    }
}
