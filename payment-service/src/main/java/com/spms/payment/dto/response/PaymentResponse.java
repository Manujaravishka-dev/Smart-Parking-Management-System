package com.spms.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.spms.payment.entity.Payment;
import com.spms.payment.entity.PaymentMethod;
import com.spms.payment.entity.PaymentStatus;

public record PaymentResponse(
        Long id,
        Long reservationId,
        Long userId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String transactionId,
        PaymentStatus status,
        LocalDateTime paymentDate,
        LocalDateTime createdAt,
        String maskedCardNumber) {

    public static PaymentResponse from(Payment payment) {
        return from(payment, null);
    }

    public static PaymentResponse from(Payment payment, String maskedCardNumber) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReservationId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getTransactionId(),
                payment.getStatus(),
                payment.getPaymentDate(),
                payment.getCreatedAt(),
                maskedCardNumber);
    }
}
