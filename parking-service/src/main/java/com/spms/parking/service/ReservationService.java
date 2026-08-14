package com.spms.parking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spms.parking.dto.request.CreateReservationRequest;
import com.spms.parking.dto.response.ReservationResponse;
import com.spms.parking.entity.ParkingSpace;
import com.spms.parking.entity.ParkingSpaceStatus;
import com.spms.parking.entity.Reservation;
import com.spms.parking.entity.ReservationStatus;
import com.spms.parking.exception.InvalidReservationStateException;
import com.spms.parking.exception.InvalidReservationTimeException;
import com.spms.parking.exception.ParkingSpaceNotAvailableException;
import com.spms.parking.exception.ParkingSpaceNotFoundException;
import com.spms.parking.exception.ReservationNotFoundException;
import com.spms.parking.repository.ParkingSpaceRepository;
import com.spms.parking.repository.ReservationRepository;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;

    public ReservationService(ReservationRepository reservationRepository,
            ParkingSpaceRepository parkingSpaceRepository) {
        this.reservationRepository = reservationRepository;
        this.parkingSpaceRepository = parkingSpaceRepository;
    }

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new InvalidReservationTimeException();
        }

        ParkingSpace space = findSpaceForUpdate(request.parkingSpaceId());
        if (space.getStatus() != ParkingSpaceStatus.AVAILABLE) {
            throw new ParkingSpaceNotAvailableException(request.parkingSpaceId());
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(request.userId());
        reservation.setVehicleId(request.vehicleId());
        reservation.setParkingSpaceId(request.parkingSpaceId());
        reservation.setStartTime(request.startTime());
        reservation.setEndTime(request.endTime());
        reservation.setStatus(ReservationStatus.CONFIRMED);

        space.setStatus(ParkingSpaceStatus.RESERVED);
        parkingSpaceRepository.save(space);

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long id) {
        return ReservationResponse.from(findReservation(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByUser(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse cancelReservation(Long id) {
        Reservation reservation = findReservation(id);
        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new InvalidReservationStateException(id);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        releaseSpace(reservation.getParkingSpaceId());

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse releaseReservation(Long id) {
        Reservation reservation = findReservation(id);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidReservationStateException(id);
        }

        reservation.setStatus(ReservationStatus.COMPLETED);
        releaseSpace(reservation.getParkingSpaceId());

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    private void releaseSpace(Long parkingSpaceId) {
        parkingSpaceRepository.findById(parkingSpaceId).ifPresent(space -> {
            if (space.getStatus() == ParkingSpaceStatus.RESERVED) {
                space.setStatus(ParkingSpaceStatus.AVAILABLE);
                parkingSpaceRepository.save(space);
            }
        });
    }

    private ParkingSpace findSpaceForUpdate(Long id) {
        return parkingSpaceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ParkingSpaceNotFoundException(id));
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }
}
