package com.spms.parking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.spms.parking.entity.ParkingSpace;
import com.spms.parking.entity.ParkingSpaceStatus;

import jakarta.persistence.LockModeType;

public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ParkingSpace s WHERE s.id = :id")
    Optional<ParkingSpace> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT s FROM ParkingSpace s
            WHERE (:city IS NULL OR LOWER(s.city) = LOWER(:city))
              AND (:zone IS NULL OR LOWER(s.zone) = LOWER(:zone))
              AND (:available IS NULL
                   OR (:available = TRUE AND s.status = :availableStatus)
                   OR (:available = FALSE AND s.status <> :availableStatus))
            ORDER BY s.id
            """)
    List<ParkingSpace> search(@Param("city") String city,
            @Param("zone") String zone,
            @Param("available") Boolean available,
            @Param("availableStatus") ParkingSpaceStatus availableStatus);
}
