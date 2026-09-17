package com.eventflow.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class MoneyUtils {

    public static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    public static final BigDecimal MAX_PRICE = new BigDecimal("999999.99");
    public static final BigDecimal MIN_TOTAL = new BigDecimal("0.01");
    public static final BigDecimal MAX_TOTAL = new BigDecimal("99899999001.00");
    public static final String CURRENCY = "USD";

    private MoneyUtils() {
    }

    public static BigDecimal validateAndNormalizePrice(BigDecimal value, String field) {
        if (value == null) {
            throw new ValidationException("Invalid request",
                    List.of(new ErrorResponse.FieldError(field, "must not be null")));
        }
        try {
            BigDecimal normalized = value.setScale(2, RoundingMode.UNNECESSARY);
            if (normalized.compareTo(MIN_PRICE) < 0 || normalized.compareTo(MAX_PRICE) > 0) {
                throw new ValidationException("Invalid request",
                        List.of(new ErrorResponse.FieldError(field,
                                "must be between 0.01 and 999999.99")));
            }
            return normalized;
        } catch (ArithmeticException ex) {
            throw new ValidationException("Invalid request",
                    List.of(new ErrorResponse.FieldError(field,
                            "must be representable with at most two decimal places without rounding")));
        }
    }

    public static BigDecimal multiply(BigDecimal unitPrice, int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.UNNECESSARY);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b).setScale(2, RoundingMode.UNNECESSARY);
    }
}
