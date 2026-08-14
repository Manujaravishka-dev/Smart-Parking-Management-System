package com.spms.payment.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.spms.payment.entity.PaymentMethod;
import com.spms.payment.entity.PaymentStatus;
import com.spms.payment.util.CardNumberUtil;

@Component
public class MockPaymentGateway {

    public static final String DECLINED_CARD_SUFFIX = "0002";

    public MockPaymentResult process(PaymentMethod paymentMethod, String cardNumber) {
        PaymentStatus status = isDeclined(paymentMethod, cardNumber) ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
        String transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return new MockPaymentResult(transactionId, status);
    }

    private boolean isDeclined(PaymentMethod paymentMethod, String cardNumber) {
        if (paymentMethod != PaymentMethod.CARD) {
            return false;
        }
        String digits = CardNumberUtil.normalize(cardNumber);
        return digits.length() >= 4 && digits.substring(digits.length() - 4).equals(DECLINED_CARD_SUFFIX);
    }

    public record MockPaymentResult(String transactionId, PaymentStatus status) {
    }
}
