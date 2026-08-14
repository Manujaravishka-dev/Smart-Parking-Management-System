package com.spms.parking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, parkingSpaceRepository);
    }

    @Test
    void createReservation_success_confirmsAndReservesSpace() {
        ParkingSpace space = space(1L, ParkingSpaceStatus.AVAILABLE);
        when(parkingSpaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));
        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(1L);
            return reservation;
        });

        LocalDateTime start = LocalDateTime.now();
        ReservationResponse response = reservationService
                .createReservation(new CreateReservationRequest(1L, 1L, 1L, start, start.plusHours(2)));

        assertEquals(1L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(1L, response.vehicleId());
        assertEquals(1L, response.parkingSpaceId());
        assertEquals(ReservationStatus.CONFIRMED, response.status());
        assertEquals(ParkingSpaceStatus.RESERVED, space.getStatus());
        verify(parkingSpaceRepository).save(space);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createReservation_spaceNotFound_throws() {
        when(parkingSpaceRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ParkingSpaceNotFoundException.class,
                () -> reservationService.createReservation(
                        new CreateReservationRequest(1L, 1L, 99L, LocalDateTime.now(), LocalDateTime.now().plusHours(1))));
    }

    @Test
    void createReservation_spaceNotAvailable_throws() {
        ParkingSpace space = space(1L, ParkingSpaceStatus.RESERVED);
        when(parkingSpaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(space));

        assertThrows(ParkingSpaceNotAvailableException.class,
                () -> reservationService.createReservation(
                        new CreateReservationRequest(1L, 1L, 1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1))));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void createReservation_invalidTime_throws() {
        LocalDateTime end = LocalDateTime.now();

        assertThrows(InvalidReservationTimeException.class,
                () -> reservationService.createReservation(
                        new CreateReservationRequest(1L, 1L, 1L, end, end.minusHours(1))));
        verify(parkingSpaceRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void createReservation_equalTime_throws() {
        LocalDateTime time = LocalDateTime.now();

        assertThrows(InvalidReservationTimeException.class,
                () -> reservationService.createReservation(
                        new CreateReservationRequest(1L, 1L, 1L, time, time)));
    }

    @Test
    void getReservation_found() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation(1L, ReservationStatus.CONFIRMED)));

        ReservationResponse response = reservationService.getReservation(1L);

        assertEquals(ReservationStatus.CONFIRMED, response.status());
        assertEquals(1L, response.userId());
    }

    @Test
    void getReservation_notFound_throws() {
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> reservationService.getReservation(99L));
    }

    @Test
    void getReservationsByUser_returnsReservations() {
        when(reservationRepository.findByUserId(1L))
                .thenReturn(List.of(reservation(1L, ReservationStatus.CONFIRMED),
                        reservation(2L, ReservationStatus.CANCELLED)));

        List<ReservationResponse> responses = reservationService.getReservationsByUser(1L);

        assertEquals(2, responses.size());
        assertEquals(ReservationStatus.CONFIRMED, responses.get(0).status());
    }

    @Test
    void getReservationsByUser_returnsEmptyList() {
        when(reservationRepository.findByUserId(99L)).thenReturn(List.of());

        assertTrue(reservationService.getReservationsByUser(99L).isEmpty());
    }

    @Test
    void cancelReservation_success_releasesSpace() {
        Reservation reservation = reservation(1L, ReservationStatus.CONFIRMED);
        ParkingSpace space = space(1L, ParkingSpaceStatus.RESERVED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(space));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.cancelReservation(1L);

        assertEquals(ReservationStatus.CANCELLED, response.status());
        assertEquals(ParkingSpaceStatus.AVAILABLE, space.getStatus());
    }

    @Test
    void cancelReservation_alreadyCancelled_throws() {
        Reservation reservation = reservation(1L, ReservationStatus.CANCELLED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(InvalidReservationStateException.class, () -> reservationService.cancelReservation(1L));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void cancelReservation_notFound_throws() {
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> reservationService.cancelReservation(99L));
    }

    @Test
    void releaseReservation_success_releasesSpace() {
        Reservation reservation = reservation(1L, ReservationStatus.CONFIRMED);
        ParkingSpace space = space(1L, ParkingSpaceStatus.RESERVED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(space));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.releaseReservation(1L);

        assertEquals(ReservationStatus.COMPLETED, response.status());
        assertEquals(ParkingSpaceStatus.AVAILABLE, space.getStatus());
    }

    @Test
    void releaseReservation_notConfirmed_throws() {
        Reservation reservation = reservation(1L, ReservationStatus.CANCELLED);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(InvalidReservationStateException.class, () -> reservationService.releaseReservation(1L));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void releaseReservation_notFound_throws() {
        when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> reservationService.releaseReservation(99L));
    }

    private ParkingSpace space(Long id, ParkingSpaceStatus status) {
        ParkingSpace space = new ParkingSpace();
        space.setId(id);
        space.setOwnerId(1L);
        space.setSpaceNumber("A-01");
        space.setLocation("Level 1");
        space.setCity("Colombo");
        space.setZone("Zone-A");
        space.setPricePerHour(new BigDecimal("2.50"));
        space.setStatus(status);
        return space;
    }

    private Reservation reservation(Long id, ReservationStatus status) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setUserId(1L);
        reservation.setVehicleId(1L);
        reservation.setParkingSpaceId(1L);
        reservation.setStartTime(LocalDateTime.now());
        reservation.setEndTime(LocalDateTime.now().plusHours(1));
        reservation.setStatus(status);
        return reservation;
    }
}
