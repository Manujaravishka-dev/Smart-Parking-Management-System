package com.spms.parking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spms.parking.entity.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);
}
