package com.eventflow.common;

public final class IdValidator {

    private IdValidator() {
    }

    public static long parsePathId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            throw new ValidationException("Invalid request",
                    java.util.List.of(new ErrorResponse.FieldError("id", "must be a positive integer")));
        }
        try {
            long value = Long.parseLong(rawId);
            if (value <= 0) {
                throw new ValidationException("Invalid request",
                        java.util.List.of(new ErrorResponse.FieldError("id", "must be a positive integer")));
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new ValidationException("Invalid request",
                    java.util.List.of(new ErrorResponse.FieldError("id", "must be a positive integer")));
        }
    }

    public static Long parseOptionalQueryId(java.util.Map<String, java.util.List<String>> params, String name) {
        java.util.List<String> values = params.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            long value = Long.parseLong(values.get(0));
            if (value <= 0) {
                throw new ValidationException("Invalid request",
                        java.util.List.of(new ErrorResponse.FieldError(name, "must be a positive integer")));
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new ValidationException("Invalid request",
                    java.util.List.of(new ErrorResponse.FieldError(name, "must be a positive integer")));
        }
    }
}
