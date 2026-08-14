package com.spms.parking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import com.spms.parking.dto.request.CreateReservationRequest;
import com.spms.parking.entity.ParkingSpace;
import com.spms.parking.entity.ParkingSpaceStatus;
import com.spms.parking.exception.ParkingSpaceNotAvailableException;
import com.spms.parking.repository.ParkingSpaceRepository;
import com.spms.parking.service.ReservationService;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:parkingconc;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ReservationConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ParkingSpaceRepository parkingSpaceRepository;

    @Test
    void concurrentReservationsForSameSpace_onlyOneSucceeds() throws Exception {
        ParkingSpace space = new ParkingSpace();
        space.setOwnerId(1L);
        space.setSpaceNumber("CONC-001");
        space.setLocation("Level 1");
        space.setCity("Colombo");
        space.setZone("Zone-A");
        space.setPricePerHour(new BigDecimal("2.50"));
        space.setStatus(ParkingSpaceStatus.AVAILABLE);
        space = parkingSpaceRepository.save(space);
        final Long spaceId = space.getId();

        int participants = 2;
        ExecutorService executor = Executors.newFixedThreadPool(participants);
        CountDownLatch ready = new CountDownLatch(participants);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < participants; i++) {
            final long userId = 100L + i;
            final long vehicleId = 200L + i;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    reservationService.createReservation(new CreateReservationRequest(
                            userId, vehicleId, spaceId,
                            LocalDateTime.now(), LocalDateTime.now().plusHours(1)));
                    return true;
                } catch (ParkingSpaceNotAvailableException e) {
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

        assertEquals(1, successes, "exactly one concurrent reservation should succeed");
        assertEquals(1, conflicts, "exactly one concurrent reservation should be rejected");
        assertEquals(ParkingSpaceStatus.RESERVED,
                parkingSpaceRepository.findById(space.getId()).orElseThrow().getStatus());
    }
}
