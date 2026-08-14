package com.spms.payment.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CardNumberUtilTest {

    @Test
    void isValid_validCard_returnsTrue() {
        assertTrue(CardNumberUtil.isValid("4111111111111111"));
    }

    @Test
    void isValid_validCardWithSpacesAndDashes_returnsTrue() {
        assertTrue(CardNumberUtil.isValid("4111 1111 1111 1111"));
        assertTrue(CardNumberUtil.isValid("4111-1111-1111-1111"));
    }

    @Test
    void isValid_failedLuhn_returnsFalse() {
        assertFalse(CardNumberUtil.isValid("4111111111111112"));
    }

    @Test
    void isValid_tooShort_returnsFalse() {
        assertFalse(CardNumberUtil.isValid("4111111111111"));
    }

    @Test
    void isValid_nonDigitCharacters_returnsFalse() {
        assertFalse(CardNumberUtil.isValid("4111-1111-1111-111a"));
    }

    @Test
    void mask_masksAllButLastFour() {
        assertEquals("************1111", CardNumberUtil.mask("4111111111111111"));
    }

    @Test
    void mask_null_returnsNull() {
        assertNull(CardNumberUtil.mask(null));
    }

    @Test
    void normalize_stripsSpacesAndDashes() {
        assertEquals("4111111111111111", CardNumberUtil.normalize("4111 1111-1111 1111"));
    }
}
