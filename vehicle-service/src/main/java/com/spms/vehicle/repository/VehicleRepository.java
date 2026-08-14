package com.spms.vehicle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.spms.vehicle.entity.Vehicle;

import jakarta.persistence.LockModeType;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.id = :id")
    Optional<Vehicle> findByIdForUpdate(@Param("id") Long id);

    List<Vehicle> findByUserId(Long userId);

    boolean existsByVehicleNumber(String vehicleNumber);
}
