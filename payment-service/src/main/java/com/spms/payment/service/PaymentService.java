package com.spms.payment.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.spms.payment.util.CardNumberUtil;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MockPaymentGateway mockPaymentGateway;
    private final ReservationClient reservationClient;

    public PaymentService(PaymentRepository paymentRepository,
            MockPaymentGateway mockPaymentGateway,
            ReservationClient reservationClient) {
        this.paymentRepository = paymentRepository;
        this.mockPaymentGateway = mockPaymentGateway;
        this.reservationClient = reservationClient;
    }

    @Transactional
    public PaymentResponse processPayment(CreatePaymentRequest request) {
        validateCard(request);

        if (!reservationClient.exists(request.reservationId())) {
            throw new ReservationNotFoundException(request.reservationId());
        }
        if (paymentRepository.existsByReservationIdAndStatus(request.reservationId(), PaymentStatus.SUCCESS)) {
            throw new DuplicatePaymentException(request.reservationId());
        }

        MockPaymentGateway.MockPaymentResult result = mockPaymentGateway.process(request.paymentMethod(),
                request.cardNumber());

        Payment payment = new Payment();
        payment.setReservationId(request.reservationId());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setTransactionId(result.transactionId());
        payment.setStatus(result.status());
        payment.setPaymentDate(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        return PaymentResponse.from(saved, CardNumberUtil.mask(request.cardNumber()));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        return PaymentResponse.from(findPayment(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceipt(Long id) {
        Payment payment = findPayment(id);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ReceiptNotAvailableException(id, payment.getStatus());
        }
        return ReceiptResponse.from(payment);
    }

    private void validateCard(CreatePaymentRequest request) {
        if (request.paymentMethod() != PaymentMethod.CARD) {
            return;
        }
        if (request.cardNumber() == null || request.cardNumber().isBlank()) {
            throw new InvalidCardDataException("cardNumber is required for CARD payments");
        }
        if (!CardNumberUtil.isValid(request.cardNumber())) {
            throw new InvalidCardDataException("cardNumber must be a valid card number");
        }
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
