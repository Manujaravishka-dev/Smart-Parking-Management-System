package com.spms.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.spms.vehicle.entity.Vehicle;
import com.spms.vehicle.entity.VehicleStatus;
import com.spms.vehicle.entity.VehicleType;
import com.spms.vehicle.exception.VehicleAlreadyInsideException;
import com.spms.vehicle.exception.VehicleNotInsideException;
import com.spms.vehicle.repository.VehicleRepository;
import com.spms.vehicle.service.VehicleService;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:vehicleconc;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VehicleConcurrencyTest {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void concurrentEntriesForSameVehicle_onlyOneSucceeds() throws Exception {
        Vehicle vehicle = new Vehicle();
        vehicle.setUserId(1L);
        vehicle.setVehicleNumber("CONC-001");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setBrand("Toyota");
        vehicle.setModel("Camry");
        vehicle.setStatus(VehicleStatus.OUTSIDE);
        vehicle = vehicleRepository.save(vehicle);
        final Long vehicleId = vehicle.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    vehicleService.vehicleEntry(vehicleId);
                    return true;
                } catch (VehicleAlreadyInsideException e) {
                    return false;
                }
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();

        int successes = 0;
        int conflicts = 0;
        for (Future<Boolean> future : futures) {
            if (Boolean.TRUE.equals(future.get(30, TimeUnit.SECONDS))) {
                successes++;
            } else {
                conflicts++;
            }
        }
        executor.shutdownNow();

        assertEquals(1, successes, "exactly one concurrent entry should succeed");
        assertEquals(1, conflicts, "exactly one concurrent entry should be rejected");
        assertEquals(VehicleStatus.INSIDE,
                vehicleRepository.findById(vehicleId).orElseThrow().getStatus());
    }

    @Test
    void concurrentExitsForSameVehicle_onlyOneSucceeds() throws Exception {
        Vehicle vehicle = new Vehicle();
        vehicle.setUserId(1L);
        vehicle.setVehicleNumber("CONC-002");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setBrand("Toyota");
        vehicle.setModel("Camry");
        vehicle.setStatus(VehicleStatus.INSIDE);
        vehicle = vehicleRepository.save(vehicle);
        final Long vehicleId = vehicle.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    vehicleService.vehicleExit(vehicleId);
                    return true;
                } catch (VehicleNotInsideException e) {
                    return false;
                }
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();

        int successes = 0;
        int conflicts = 0;
        for (Future<Boolean> future : futures) {
            if (Boolean.TRUE.equals(future.get(30, TimeUnit.SECONDS))) {
                successes++;
            } else {
                conflicts++;
            }
        }
        executor.shutdownNow();

        assertEquals(1, successes, "exactly one concurrent exit should succeed");
        assertEquals(1, conflicts, "exactly one concurrent exit should be rejected");
        assertEquals(VehicleStatus.OUTSIDE,
                vehicleRepository.findById(vehicleId).orElseThrow().getStatus());
    }
}
