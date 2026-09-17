package com.eventflow.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PaginationParams(int page, int size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public long offset() {
        return (long) page * size;
    }

    public static PaginationParams parse(Map<String, List<String>> queryParams, Set<String> allowedParams) {
        List<ErrorResponse.FieldError> errors = new ArrayList<>();

        for (String key : queryParams.keySet()) {
            if (!allowedParams.contains(key)) {
                errors.add(new ErrorResponse.FieldError(key, "unknown query parameter"));
            }
        }

        for (String key : allowedParams) {
            List<String> values = queryParams.get(key);
            if (values != null && values.size() > 1) {
                errors.add(new ErrorResponse.FieldError(key, "must not be repeated"));
            }
            if (values != null && values.size() == 1 && values.get(0).isEmpty()) {
                errors.add(new ErrorResponse.FieldError(key, "must not be empty"));
            }
        }

        int page = DEFAULT_PAGE;
        int size = DEFAULT_SIZE;

        List<String> pageValues = queryParams.get("page");
        if (pageValues != null && !pageValues.isEmpty()) {
            try {
                page = Integer.parseInt(pageValues.get(0));
                if (page < 0 || page > Integer.MAX_VALUE) {
                    errors.add(new ErrorResponse.FieldError("page", "must be between 0 and 2147483647"));
                }
            } catch (NumberFormatException ex) {
                errors.add(new ErrorResponse.FieldError("page", "must be a valid integer"));
            }
        }

        List<String> sizeValues = queryParams.get("size");
        if (sizeValues != null && !sizeValues.isEmpty()) {
            try {
                size = Integer.parseInt(sizeValues.get(0));
                if (size < 1 || size > MAX_SIZE) {
                    errors.add(new ErrorResponse.FieldError("size", "must be between 1 and 100"));
                }
            } catch (NumberFormatException ex) {
                errors.add(new ErrorResponse.FieldError("size", "must be a valid integer"));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid request", errors);
        }

        return new PaginationParams(page, size);
    }
}
