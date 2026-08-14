package com.spms.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spms.payment.client.MockPaymentGateway;
import com.spms.payment.client.ReservationClient;
import com.spms.payment.dto.request.CreatePaymentRequest;
import com.spms.payment.dto.response.PaymentResponse;
import com.spms.payment.dto.response.ReceiptResponse;
import com.spms.payment.entity.Payment;
import com.spms.payment.entity.PaymentMethod;
import com.spms.payment.entity.PaymentStatus;
import com.spms.payment.exception.DuplicatePaymentException;
import com.spms.payment.exception.InvalidCardDataException;
import com.spms.payment.exception.PaymentNotFoundException;
import com.spms.payment.exception.ReceiptNotAvailableException;
import com.spms.payment.exception.ReservationNotFoundException;
import com.spms.payment.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MockPaymentGateway mockPaymentGateway;

    @Mock
    private ReservationClient reservationClient;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, mockPaymentGateway, reservationClient);
    }

    @Test
    void processPayment_cardSuccess_returnsSuccessAndMaskedCard() {
        CreatePaymentRequest request = request(1L, 1L, "500", PaymentMethod.CARD, "4111111111111111");

        when(reservationClient.exists(1L)).thenReturn(true);
        when(paymentRepository.existsByReservationIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(mockPaymentGateway.process(PaymentMethod.CARD, "4111111111111111"))
                .thenReturn(new MockPaymentGateway.MockPaymentResult("TXN-ABC", PaymentStatus.SUCCESS));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(10L);
            return payment;
        });

        PaymentResponse response = paymentService.processPayment(request);

        assertEquals(10L, response.id());
        assertEquals(1L, response.reservationId());
        assertEquals(new BigDecimal("500"), response.amount());
        assertEquals(PaymentMethod.CARD, response.paymentMethod());
        assertEquals("TXN-ABC", response.transactionId());
        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertEquals("************1111", response.maskedCardNumber());
        assertEquals(PaymentStatus.SUCCESS, response.status());
    }

    @Test
    void processPayment_declinedCard_returnsFailed() {
        CreatePaymentRequest request = request(1L, 1L, "500", PaymentMethod.CARD, "4000000000000002");

        when(reservationClient.exists(1L)).thenReturn(true);
        when(paymentRepository.existsByReservationIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(mockPaymentGateway.process(PaymentMethod.CARD, "4000000000000002"))
                .thenReturn(new MockPaymentGateway.MockPaymentResult("TXN-DEC", PaymentStatus.FAILED));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(11L);
            return payment;
        });

        PaymentResponse response = paymentService.processPayment(request);

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals("************0002", response.maskedCardNumber());
    }

    @Test
    void processPayment_cash_returnsSuccessWithoutCard() {
        CreatePaymentRequest request = request(1L, 1L, "500", PaymentMethod.CASH, null);

        when(reservationClient.exists(1L)).thenReturn(true);
        when(paymentRepository.existsByReservationIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(mockPaymentGateway.process(PaymentMethod.CASH, null))
                .thenReturn(new MockPaymentGateway.MockPaymentResult("TXN-CASH", PaymentStatus.SUCCESS));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(12L);
            return payment;
        });

        PaymentResponse response = paymentService.processPayment(request);

        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertNull(response.maskedCardNumber());
    }

    @Test
    void processPayment_invalidCard_throws() {
        CreatePaymentRequest request = request(1L, 1L, "500", PaymentMethod.CARD, "4111111111111112");

        assertThrows(InvalidCardDataException.class, () -> paymentService.processPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_missingCardForCardMethod_throws() {
        CreatePaymentRequest request = request(1L, 1L, "500", PaymentMethod.CARD, null);

        assertThrows(InvalidCardDataException.class, () -> paymentService.processPayment(request));
    }

    @Test
    void processPayment_reservationNotFound_throws() {
        CreatePaymentRequest request = request(1L, 1L, "500", PaymentMethod.CARD, "4111111111111111");

        when(reservationClient.exists(1L)).thenReturn(false);

        assertThrows(ReservationNotFoundException.class, () -> paymentService.processPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_duplicateSuccessfulPayment_throws() {
        CreatePaymentRequest request = request(1L, 1L, "500", PaymentMethod.CARD, "4111111111111111");

        when(reservationClient.exists(1L)).thenReturn(true);
        when(paymentRepository.existsByReservationIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(true);

        assertThrows(DuplicatePaymentException.class, () -> paymentService.processPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void processPayment_retryAllowedAfterFailed() {
        CreatePaymentRequest request = request(1L, 1L, "500", PaymentMethod.CARD, "4111111111111111");

        when(reservationClient.exists(1L)).thenReturn(true);
        when(paymentRepository.existsByReservationIdAndStatus(1L, PaymentStatus.SUCCESS)).thenReturn(false);
        when(mockPaymentGateway.process(PaymentMethod.CARD, "4111111111111111"))
                .thenReturn(new MockPaymentGateway.MockPaymentResult("TXN-FAIL", PaymentStatus.FAILED))
                .thenReturn(new MockPaymentGateway.MockPaymentResult("TXN-OK", PaymentStatus.SUCCESS));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(13L);
            return payment;
        });

        PaymentResponse first = paymentService.processPayment(request);
        PaymentResponse second = paymentService.processPayment(request);

        assertEquals(PaymentStatus.FAILED, first.status());
        assertEquals(PaymentStatus.SUCCESS, second.status());
    }

    @Test
    void getPayment_found() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(1L, PaymentStatus.SUCCESS)));

        PaymentResponse response = paymentService.getPayment(1L);

        assertEquals(1L, response.id());
        assertEquals(PaymentStatus.SUCCESS, response.status());
    }

    @Test
    void getPayment_notFound_throws() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPayment(99L));
    }

    @Test
    void getPaymentsByReservation_returnsList() {
        when(paymentRepository.findByReservationId(1L))
                .thenReturn(List.of(payment(1L, PaymentStatus.FAILED), payment(2L, PaymentStatus.SUCCESS)));

        List<PaymentResponse> responses = paymentService.getPaymentsByReservation(1L);

        assertEquals(2, responses.size());
        assertEquals(PaymentStatus.FAILED, responses.get(0).status());
        assertEquals(PaymentStatus.SUCCESS, responses.get(1).status());
    }

    @Test
    void getPaymentsByUser_returnsList() {
        when(paymentRepository.findByUserId(1L)).thenReturn(List.of(payment(1L, PaymentStatus.SUCCESS)));

        List<PaymentResponse> responses = paymentService.getPaymentsByUser(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).userId());
    }

    @Test
    void getReceipt_success_returnsReceipt() {
        Payment payment = payment(1L, PaymentStatus.SUCCESS);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        ReceiptResponse receipt = paymentService.getReceipt(1L);

        assertEquals(1L, receipt.receiptId());
        assertEquals(payment.getTransactionId(), receipt.transactionId());
        assertEquals(payment.getReservationId(), receipt.reservationId());
        assertEquals(payment.getUserId(), receipt.userId());
        assertEquals(payment.getAmount(), receipt.amount());
        assertEquals(payment.getPaymentMethod(), receipt.paymentMethod());
        assertEquals(PaymentStatus.SUCCESS, receipt.paymentStatus());
        assertEquals(payment.getPaymentDate(), receipt.paymentDate());
    }

    @Test
    void getReceipt_failedPayment_throws() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(1L, PaymentStatus.FAILED)));

        assertThrows(ReceiptNotAvailableException.class, () -> paymentService.getReceipt(1L));
    }

    @Test
    void getReceipt_notFound_throws() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getReceipt(99L));
    }

    private CreatePaymentRequest request(Long reservationId, Long userId, String amount,
            PaymentMethod method, String cardNumber) {
        return new CreatePaymentRequest(reservationId, userId, new BigDecimal(amount), method, cardNumber);
    }

    private Payment payment(Long id, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setReservationId(1L);
        payment.setUserId(1L);
        payment.setAmount(new BigDecimal("500"));
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setTransactionId("TXN-" + id);
        payment.setStatus(status);
        payment.setPaymentDate(LocalDateTime.of(2026, 8, 14, 10, 30));
        return payment;
    }
}
