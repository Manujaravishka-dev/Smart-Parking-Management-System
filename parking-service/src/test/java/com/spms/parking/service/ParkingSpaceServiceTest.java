package com.spms.parking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spms.parking.dto.request.CreateParkingSpaceRequest;
import com.spms.parking.dto.request.UpdateParkingSpaceRequest;
import com.spms.parking.dto.request.UpdateParkingSpaceStatusRequest;
import com.spms.parking.dto.response.ParkingSpaceResponse;
import com.spms.parking.entity.ParkingSpace;
import com.spms.parking.entity.ParkingSpaceStatus;
import com.spms.parking.exception.ParkingSpaceNotFoundException;
import com.spms.parking.repository.ParkingSpaceRepository;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceServiceTest {

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    private ParkingSpaceService parkingSpaceService;

    @BeforeEach
    void setUp() {
        parkingSpaceService = new ParkingSpaceService(parkingSpaceRepository);
    }

    @Test
    void createSpace_setsAvailableStatus() {
        CreateParkingSpaceRequest request = new CreateParkingSpaceRequest(
                1L, "A-01", "Level 1, Near Gate A", "Colombo", "Zone-A",
                new BigDecimal("2.50"));

        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenAnswer(invocation -> {
            ParkingSpace space = invocation.getArgument(0);
            space.setId(1L);
            return space;
        });

        ParkingSpaceResponse response = parkingSpaceService.createSpace(request);

        assertEquals(1L, response.id());
        assertEquals(1L, response.ownerId());
        assertEquals("A-01", response.spaceNumber());
        assertEquals("Colombo", response.city());
        assertEquals("Zone-A", response.zone());
        assertEquals(new BigDecimal("2.50"), response.pricePerHour());
        assertEquals(ParkingSpaceStatus.AVAILABLE, response.status());
    }

    @Test
    void getSpace_found() {
        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(space(1L)));

        ParkingSpaceResponse response = parkingSpaceService.getSpace(1L);

        assertEquals("A-01", response.spaceNumber());
        assertEquals(ParkingSpaceStatus.AVAILABLE, response.status());
    }

    @Test
    void getSpace_notFound_throws() {
        when(parkingSpaceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ParkingSpaceNotFoundException.class, () -> parkingSpaceService.getSpace(99L));
    }

    @Test
    void searchSpaces_noFilters() {
        when(parkingSpaceRepository.search(null, null, null, ParkingSpaceStatus.AVAILABLE))
                .thenReturn(List.of(space(1L), space(2L)));

        List<ParkingSpaceResponse> responses = parkingSpaceService.searchSpaces(null, null, null);

        assertEquals(2, responses.size());
        verify(parkingSpaceRepository).search(null, null, null, ParkingSpaceStatus.AVAILABLE);
    }

    @Test
    void searchSpaces_withFilters() {
        when(parkingSpaceRepository.search(eq("Colombo"), eq("Zone-A"), eq(true), eq(ParkingSpaceStatus.AVAILABLE)))
                .thenReturn(List.of(space(1L)));

        List<ParkingSpaceResponse> responses = parkingSpaceService.searchSpaces("Colombo", "Zone-A", true);

        assertEquals(1, responses.size());
        assertEquals("Colombo", responses.get(0).city());
    }

    @Test
    void updateSpace_updatesFields() {
        ParkingSpace space = space(1L);
        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(space));
        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateParkingSpaceRequest request = new UpdateParkingSpaceRequest(
                2L, "B-02", "Level 2", "Galle", "Zone-B", new BigDecimal("3.75"));
        ParkingSpaceResponse response = parkingSpaceService.updateSpace(1L, request);

        assertEquals(2L, response.ownerId());
        assertEquals("B-02", response.spaceNumber());
        assertEquals("Galle", response.city());
        assertEquals("Zone-B", response.zone());
        assertEquals(new BigDecimal("3.75"), response.pricePerHour());
    }

    @Test
    void updateSpace_notFound_throws() {
        when(parkingSpaceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ParkingSpaceNotFoundException.class,
                () -> parkingSpaceService.updateSpace(99L, new UpdateParkingSpaceRequest(null, null, null, null, null, null)));
    }

    @Test
    void deleteSpace_deletes() {
        ParkingSpace space = space(1L);
        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(space));

        parkingSpaceService.deleteSpace(1L);

        verify(parkingSpaceRepository).delete(space);
    }

    @Test
    void deleteSpace_notFound_throws() {
        when(parkingSpaceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ParkingSpaceNotFoundException.class, () -> parkingSpaceService.deleteSpace(99L));
    }

    @Test
    void updateStatus_updates() {
        ParkingSpace space = space(1L);
        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(space));
        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParkingSpaceResponse response = parkingSpaceService.updateStatus(1L,
                new UpdateParkingSpaceStatusRequest(ParkingSpaceStatus.OCCUPIED));

        assertEquals(ParkingSpaceStatus.OCCUPIED, response.status());
    }

    @Test
    void updateStatus_notFound_throws() {
        when(parkingSpaceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ParkingSpaceNotFoundException.class,
                () -> parkingSpaceService.updateStatus(99L, new UpdateParkingSpaceStatusRequest(ParkingSpaceStatus.MAINTENANCE)));
    }

    private ParkingSpace space(Long id) {
        ParkingSpace space = new ParkingSpace();
        space.setId(id);
        space.setOwnerId(1L);
        space.setSpaceNumber("A-01");
        space.setLocation("Level 1, Near Gate A");
        space.setCity("Colombo");
        space.setZone("Zone-A");
        space.setPricePerHour(new BigDecimal("2.50"));
        space.setStatus(ParkingSpaceStatus.AVAILABLE);
        return space;
    }
}
