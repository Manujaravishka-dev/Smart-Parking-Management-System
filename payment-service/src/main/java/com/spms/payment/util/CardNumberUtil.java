package com.spms.payment.util;

public final class CardNumberUtil {

    private static final int MIN_LENGTH = 13;
    private static final int MAX_LENGTH = 19;

    private CardNumberUtil() {
    }

    public static boolean isValid(String cardNumber) {
        String digits = normalize(cardNumber);
        if (digits.length() < MIN_LENGTH || digits.length() > MAX_LENGTH) {
            return false;
        }
        return passesLuhn(digits);
    }

    public static String normalize(String cardNumber) {
        if (cardNumber == null) {
            return "";
        }
        return cardNumber.replaceAll("[\\s-]", "");
    }

    public static String mask(String cardNumber) {
        String digits = normalize(cardNumber);
        if (digits.isEmpty()) {
            return null;
        }
        if (digits.length() < 4) {
            return "****";
        }
        String lastFour = digits.substring(digits.length() - 4);
        return "*".repeat(Math.max(0, digits.length() - 4)) + lastFour;
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            char c = digits.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            int value = c - '0';
            if (doubleDigit) {
                value *= 2;
                if (value > 9) {
                    value -= 9;
                }
            }
            sum += value;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
