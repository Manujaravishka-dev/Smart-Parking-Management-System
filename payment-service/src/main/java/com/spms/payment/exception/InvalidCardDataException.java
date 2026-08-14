package com.spms.payment.exception;

public class InvalidCardDataException extends RuntimeException {

    public InvalidCardDataException(String message) {
        super(message);
    }
}
