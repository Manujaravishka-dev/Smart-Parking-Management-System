package com.spms.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.spms.payment.entity.Payment;
import com.spms.payment.entity.PaymentMethod;
import com.spms.payment.entity.PaymentStatus;

public record ReceiptResponse(
        Long receiptId,
        String transactionId,
        Long reservationId,
        Long userId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        LocalDateTime paymentDate) {

    public static ReceiptResponse from(Payment payment) {
        return new ReceiptResponse(
                payment.getId(),
                payment.getTransactionId(),
                payment.getReservationId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getPaymentDate());
    }
}
