package com.spms.vehicle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spms.vehicle.dto.request.RegisterVehicleRequest;
import com.spms.vehicle.dto.request.UpdateVehicleRequest;
import com.spms.vehicle.dto.response.VehicleResponse;
import com.spms.vehicle.entity.Vehicle;
import com.spms.vehicle.entity.VehicleStatus;
import com.spms.vehicle.entity.VehicleType;
import com.spms.vehicle.exception.DuplicateVehicleNumberException;
import com.spms.vehicle.exception.VehicleAlreadyInsideException;
import com.spms.vehicle.exception.VehicleNotInsideException;
import com.spms.vehicle.exception.VehicleNotFoundException;
import com.spms.vehicle.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleService(vehicleRepository);
    }

    @Test
    void register_createsVehicleWithOutsideStatus() {
        RegisterVehicleRequest request = new RegisterVehicleRequest(
                1L, "ABC-1234", VehicleType.CAR, "Toyota", "Camry");

        when(vehicleRepository.existsByVehicleNumber("ABC-1234")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(1L);
            return vehicle;
        });

        VehicleResponse response = vehicleService.register(request);

        assertEquals(1L, response.id());
        assertEquals(1L, response.userId());
        assertEquals("ABC-1234", response.vehicleNumber());
        assertEquals(VehicleType.CAR, response.vehicleType());
        assertEquals("Toyota", response.brand());
        assertEquals("Camry", response.model());
        assertEquals(VehicleStatus.OUTSIDE, response.status());
        assertNull(response.entryTime());
        assertNull(response.exitTime());
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void register_normalizesVehicleNumber() {
        RegisterVehicleRequest request = new RegisterVehicleRequest(
                1L, "  abc-1234 ", VehicleType.MOTORCYCLE, "Honda", "CBR");

        when(vehicleRepository.existsByVehicleNumber("ABC-1234")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.register(request);

        assertEquals("ABC-1234", response.vehicleNumber());
        assertEquals(VehicleType.MOTORCYCLE, response.vehicleType());
    }

    @Test
    void register_duplicateVehicleNumber_throws() {
        RegisterVehicleRequest request = new RegisterVehicleRequest(
                1L, "ABC-1234", VehicleType.CAR, "Toyota", "Camry");

        when(vehicleRepository.existsByVehicleNumber("ABC-1234")).thenReturn(true);

        assertThrows(DuplicateVehicleNumberException.class, () -> vehicleService.register(request));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void getVehicle_found() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle(1L, "ABC-1234")));

        VehicleResponse response = vehicleService.getVehicle(1L);

        assertEquals("ABC-1234", response.vehicleNumber());
        assertEquals(VehicleStatus.OUTSIDE, response.status());
    }

    @Test
    void getVehicle_notFound_throws() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.getVehicle(99L));
    }

    @Test
    void getVehiclesByUser_returnsVehicles() {
        when(vehicleRepository.findByUserId(1L))
                .thenReturn(List.of(vehicle(1L, "ABC-1234"), vehicle(2L, "XYZ-9876")));

        List<VehicleResponse> responses = vehicleService.getVehiclesByUser(1L);

        assertEquals(2, responses.size());
        assertEquals("ABC-1234", responses.get(0).vehicleNumber());
        assertEquals("XYZ-9876", responses.get(1).vehicleNumber());
    }

    @Test
    void getVehiclesByUser_returnsEmptyList() {
        when(vehicleRepository.findByUserId(99L)).thenReturn(List.of());

        assertTrue(vehicleService.getVehiclesByUser(99L).isEmpty());
    }

    @Test
    void updateVehicle_updatesFields() {
        Vehicle vehicle = vehicle(1L, "ABC-1234");
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateVehicleRequest request = new UpdateVehicleRequest(2L, "NEW-0001", VehicleType.TRUCK, "Volvo", "FH16");
        VehicleResponse response = vehicleService.updateVehicle(1L, request);

        assertEquals(2L, response.userId());
        assertEquals("NEW-0001", response.vehicleNumber());
        assertEquals(VehicleType.TRUCK, response.vehicleType());
        assertEquals("Volvo", response.brand());
        assertEquals("FH16", response.model());
    }

    @Test
    void updateVehicle_changeVehicleNumber_conflictThrows() {
        Vehicle vehicle = vehicle(1L, "ABC-1234");
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByVehicleNumber("TAKEN-99")).thenReturn(true);

        UpdateVehicleRequest request = new UpdateVehicleRequest(null, "taken-99", null, null, null);

        assertThrows(DuplicateVehicleNumberException.class, () -> vehicleService.updateVehicle(1L, request));
    }

    @Test
    void updateVehicle_notFound_throws() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class,
                () -> vehicleService.updateVehicle(99L, new UpdateVehicleRequest(null, null, null, null, null)));
    }

    @Test
    void deleteVehicle_deletes() {
        Vehicle vehicle = vehicle(1L, "ABC-1234");
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        vehicleService.deleteVehicle(1L);

        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void deleteVehicle_notFound_throws() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.deleteVehicle(99L));
    }

    @Test
    void vehicleEntry_setsInsideAndEntryTime() {
        Vehicle vehicle = vehicle(1L, "ABC-1234");
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.vehicleEntry(1L);

        assertEquals(VehicleStatus.INSIDE, response.status());
        assertNotNull(response.entryTime());
    }

    @Test
    void vehicleEntry_alreadyInside_throws() {
        Vehicle vehicle = vehicle(1L, "ABC-1234");
        vehicle.setStatus(VehicleStatus.INSIDE);
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(vehicle));

        assertThrows(VehicleAlreadyInsideException.class, () -> vehicleService.vehicleEntry(1L));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void vehicleEntry_notFound_throws() {
        when(vehicleRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.vehicleEntry(99L));
    }

    @Test
    void vehicleExit_setsOutsideAndExitTime() {
        Vehicle vehicle = vehicle(1L, "ABC-1234");
        vehicle.setStatus(VehicleStatus.INSIDE);
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.vehicleExit(1L);

        assertEquals(VehicleStatus.OUTSIDE, response.status());
        assertNotNull(response.exitTime());
    }

    @Test
    void vehicleExit_notInside_throws() {
        Vehicle vehicle = vehicle(1L, "ABC-1234");
        when(vehicleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(vehicle));

        assertThrows(VehicleNotInsideException.class, () -> vehicleService.vehicleExit(1L));
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void vehicleExit_notFound_throws() {
        when(vehicleRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.vehicleExit(99L));
    }

    private Vehicle vehicle(Long id, String vehicleNumber) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setUserId(1L);
        vehicle.setVehicleNumber(vehicleNumber);
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setBrand("Toyota");
        vehicle.setModel("Camry");
        vehicle.setStatus(VehicleStatus.OUTSIDE);
        vehicle.setEntryTime(null);
        vehicle.setExitTime(null);
        return vehicle;
    }
}
