package com.spms.parking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spms.parking.dto.request.CreateReservationRequest;
import com.spms.parking.dto.response.ReservationResponse;
import com.spms.parking.service.ReservationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/parking/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservation(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(reservationService.getReservationsByUser(userId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<ReservationResponse> releaseReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.releaseReservation(id));
    }
}
