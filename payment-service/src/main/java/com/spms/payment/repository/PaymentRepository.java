package com.spms.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spms.payment.entity.Payment;
import com.spms.payment.entity.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByReservationId(Long reservationId);

    List<Payment> findByUserId(Long userId);

    boolean existsByReservationIdAndStatus(Long reservationId, PaymentStatus status);
}
