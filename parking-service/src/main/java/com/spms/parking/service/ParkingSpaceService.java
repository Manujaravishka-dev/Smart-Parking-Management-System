package com.spms.parking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spms.parking.dto.request.CreateParkingSpaceRequest;
import com.spms.parking.dto.request.UpdateParkingSpaceRequest;
import com.spms.parking.dto.request.UpdateParkingSpaceStatusRequest;
import com.spms.parking.dto.response.ParkingSpaceResponse;
import com.spms.parking.entity.ParkingSpace;
import com.spms.parking.entity.ParkingSpaceStatus;
import com.spms.parking.exception.ParkingSpaceNotFoundException;
import com.spms.parking.repository.ParkingSpaceRepository;

@Service
public class ParkingSpaceService {

    private final ParkingSpaceRepository parkingSpaceRepository;

    public ParkingSpaceService(ParkingSpaceRepository parkingSpaceRepository) {
        this.parkingSpaceRepository = parkingSpaceRepository;
    }

    @Transactional
    public ParkingSpaceResponse createSpace(CreateParkingSpaceRequest request) {
        ParkingSpace space = new ParkingSpace();
        space.setOwnerId(request.ownerId());
        space.setSpaceNumber(request.spaceNumber().trim());
        space.setLocation(request.location().trim());
        space.setCity(request.city().trim());
        space.setZone(request.zone().trim());
        space.setPricePerHour(request.pricePerHour());
        space.setStatus(ParkingSpaceStatus.AVAILABLE);

        return ParkingSpaceResponse.from(parkingSpaceRepository.save(space));
    }

    @Transactional(readOnly = true)
    public ParkingSpaceResponse getSpace(Long id) {
        return ParkingSpaceResponse.from(findSpace(id));
    }

    @Transactional(readOnly = true)
    public List<ParkingSpaceResponse> searchSpaces(String city, String zone, Boolean available) {
        return parkingSpaceRepository.search(city, zone, available, ParkingSpaceStatus.AVAILABLE).stream()
                .map(ParkingSpaceResponse::from)
                .toList();
    }

    @Transactional
    public ParkingSpaceResponse updateSpace(Long id, UpdateParkingSpaceRequest request) {
        ParkingSpace space = findSpace(id);

        if (request.ownerId() != null) {
            space.setOwnerId(request.ownerId());
        }
        if (request.spaceNumber() != null && !request.spaceNumber().isBlank()) {
            space.setSpaceNumber(request.spaceNumber().trim());
        }
        if (request.location() != null && !request.location().isBlank()) {
            space.setLocation(request.location().trim());
        }
        if (request.city() != null && !request.city().isBlank()) {
            space.setCity(request.city().trim());
        }
        if (request.zone() != null && !request.zone().isBlank()) {
            space.setZone(request.zone().trim());
        }
        if (request.pricePerHour() != null) {
            space.setPricePerHour(request.pricePerHour());
        }

        return ParkingSpaceResponse.from(parkingSpaceRepository.save(space));
    }

    @Transactional
    public void deleteSpace(Long id) {
        parkingSpaceRepository.delete(findSpace(id));
    }

    @Transactional
    public ParkingSpaceResponse updateStatus(Long id, UpdateParkingSpaceStatusRequest request) {
        ParkingSpace space = findSpace(id);
        space.setStatus(request.status());
        return ParkingSpaceResponse.from(parkingSpaceRepository.save(space));
    }

    private ParkingSpace findSpace(Long id) {
        return parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ParkingSpaceNotFoundException(id));
    }
}
