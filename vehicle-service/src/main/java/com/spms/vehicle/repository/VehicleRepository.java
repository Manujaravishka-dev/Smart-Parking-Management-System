package com.spms.vehicle.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.spms.vehicle.entity.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserId(Long userId);

    boolean existsByVehicleNumber(String vehicleNumber);
}
