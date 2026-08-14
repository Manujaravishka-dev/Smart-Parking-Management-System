package com.spms.payment.exception;

import com.spms.payment.entity.PaymentStatus;

public class ReceiptNotAvailableException extends RuntimeException {

    public ReceiptNotAvailableException(Long id, PaymentStatus status) {
        super("Receipt is only available for successful payments. Payment id: " + id + ", status: " + status);
    }
}
