package com.eventflow.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyUtilsTest {

    @Test
    void acceptsExactTwoDecimalScale() {
        BigDecimal normalized = MoneyUtils.validateAndNormalizePrice(new BigDecimal("1.5"), "price");
        assertEquals(new BigDecimal("1.50"), normalized);
    }

    @Test
    void rejectsRoundingRequiredValues() {
        assertThrows(ValidationException.class,
                () -> MoneyUtils.validateAndNormalizePrice(new BigDecimal("1.501"), "price"));
    }
}
