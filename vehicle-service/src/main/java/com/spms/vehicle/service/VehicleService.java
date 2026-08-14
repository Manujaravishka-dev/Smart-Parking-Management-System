package com.spms.vehicle.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.spms.vehicle.dto.request.RegisterVehicleRequest;
import com.spms.vehicle.dto.request.UpdateVehicleRequest;
import com.spms.vehicle.dto.response.VehicleResponse;
import com.spms.vehicle.entity.Vehicle;
import com.spms.vehicle.entity.VehicleStatus;
import com.spms.vehicle.exception.DuplicateVehicleNumberException;
import com.spms.vehicle.exception.VehicleAlreadyInsideException;
import com.spms.vehicle.exception.VehicleNotInsideException;
import com.spms.vehicle.exception.VehicleNotFoundException;
import com.spms.vehicle.repository.VehicleRepository;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public VehicleResponse register(RegisterVehicleRequest request) {
        String vehicleNumber = normalize(request.vehicleNumber());
        if (vehicleRepository.existsByVehicleNumber(vehicleNumber)) {
            throw new DuplicateVehicleNumberException(vehicleNumber);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setUserId(request.userId());
        vehicle.setVehicleNumber(vehicleNumber);
        vehicle.setVehicleType(request.vehicleType());
        vehicle.setBrand(request.brand().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setStatus(VehicleStatus.OUTSIDE);

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long id) {
        return VehicleResponse.from(findVehicle(id));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByUser(Long userId) {
        return vehicleRepository.findByUserId(userId).stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        Vehicle vehicle = findVehicle(id);

        if (request.userId() != null) {
            vehicle.setUserId(request.userId());
        }
        if (request.vehicleNumber() != null && !request.vehicleNumber().isBlank()) {
            String vehicleNumber = normalize(request.vehicleNumber());
            if (!vehicleNumber.equalsIgnoreCase(vehicle.getVehicleNumber())) {
                if (vehicleRepository.existsByVehicleNumber(vehicleNumber)) {
                    throw new DuplicateVehicleNumberException(vehicleNumber);
                }
                vehicle.setVehicleNumber(vehicleNumber);
            }
        }
        if (request.vehicleType() != null) {
            vehicle.setVehicleType(request.vehicleType());
        }
        if (request.brand() != null && !request.brand().isBlank()) {
            vehicle.setBrand(request.brand().trim());
        }
        if (request.model() != null && !request.model().isBlank()) {
            vehicle.setModel(request.model().trim());
        }

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(Long id) {
        vehicleRepository.delete(findVehicle(id));
    }

    @Transactional
    public VehicleResponse vehicleEntry(Long id) {
        Vehicle vehicle = findVehicle(id);
        if (vehicle.getStatus() == VehicleStatus.INSIDE) {
            throw new VehicleAlreadyInsideException(id);
        }

        vehicle.setStatus(VehicleStatus.INSIDE);
        vehicle.setEntryTime(LocalDateTime.now());

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse vehicleExit(Long id) {
        Vehicle vehicle = findVehicle(id);
        if (vehicle.getStatus() != VehicleStatus.INSIDE) {
            throw new VehicleNotInsideException(id);
        }

        vehicle.setStatus(VehicleStatus.OUTSIDE);
        vehicle.setExitTime(LocalDateTime.now());

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    private Vehicle findVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    private String normalize(String vehicleNumber) {
        return vehicleNumber.trim().toUpperCase();
    }
}
