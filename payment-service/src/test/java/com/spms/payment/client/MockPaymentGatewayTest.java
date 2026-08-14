package com.spms.payment.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.spms.payment.entity.PaymentMethod;
import com.spms.payment.entity.PaymentStatus;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    void process_validCard_returnsSuccess() {
        MockPaymentGateway.MockPaymentResult result = gateway.process(PaymentMethod.CARD, "4111111111111111");

        assertEquals(PaymentStatus.SUCCESS, result.status());
        assertNotNull(result.transactionId());
        assertTrue(result.transactionId().startsWith("TXN-"));
    }

    @Test
    void process_declinedCard_returnsFailed() {
        MockPaymentGateway.MockPaymentResult result = gateway.process(PaymentMethod.CARD, "4000000000000002");

        assertEquals(PaymentStatus.FAILED, result.status());
        assertNotNull(result.transactionId());
    }

    @Test
    void process_cash_returnsSuccess() {
        MockPaymentGateway.MockPaymentResult result = gateway.process(PaymentMethod.CASH, null);

        assertEquals(PaymentStatus.SUCCESS, result.status());
    }

    @Test
    void process_mockWallet_returnsSuccess() {
        MockPaymentGateway.MockPaymentResult result = gateway.process(PaymentMethod.MOCK_WALLET, null);

        assertEquals(PaymentStatus.SUCCESS, result.status());
    }
}
