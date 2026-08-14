package com.spms.parking.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:parkingdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ReservationControllerTest {

    private static final String START = "2026-08-15T10:00:00";
    private static final String END = "2026-08-15T12:00:00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReservation_returns201AndReservesSpace() throws Exception {
        long spaceId = createSpace();

        String body = reservationBody(1L, 1L, spaceId, START, END);

        mockMvc.perform(post("/parking/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.vehicleId", is(1)))
                .andExpect(jsonPath("$.parkingSpaceId", is((int) spaceId)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        mockMvc.perform(get("/parking/spaces/{id}", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESERVED")));
    }

    @Test
    void createReservation_spaceNotFound_returns404() throws Exception {
        String body = reservationBody(1L, 1L, 999999L, START, END);

        mockMvc.perform(post("/parking/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReservation_doubleReservation_returns409() throws Exception {
        long spaceId = createSpace();
        long reservationId = createReservation(spaceId, 1L, 1L, START, END);

        String body = reservationBody(2L, 2L, spaceId, START, END);

        mockMvc.perform(post("/parking/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("not available")));

        mockMvc.perform(get("/parking/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    void createReservation_spaceOccupied_returns409() throws Exception {
        long spaceId = createSpace();

        String statusBody = """
                {"status":"OCCUPIED"}
                """;

        mockMvc.perform(put("/parking/spaces/{id}/status", spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody))
                .andExpect(status().isOk());

        String body = reservationBody(1L, 1L, spaceId, START, END);

        mockMvc.perform(post("/parking/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("not available")));
    }

    @Test
    void createReservation_malformedJson_returns400() throws Exception {
        String body = """
                {"userId":1,"vehicleId":1,"parkingSpaceId":1,
                """;

        mockMvc.perform(post("/parking/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReservation_invalidId_returns400() throws Exception {
        mockMvc.perform(get("/parking/reservations/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReservation_invalidTime_returns400() throws Exception {
        long spaceId = createSpace();

        String body = reservationBody(1L, 1L, spaceId, END, START);

        mockMvc.perform(post("/parking/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("startTime")));
    }

    @Test
    void createReservation_missingFields_returns400() throws Exception {
        String body = """
                {"startTime":"%s","endTime":"%s"}
                """.formatted(START, END);

        mockMvc.perform(post("/parking/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("userId")));
    }

    @Test
    void getReservation_returns200() throws Exception {
        long spaceId = createSpace();
        long reservationId = createReservation(spaceId, 1L, 1L, START, END);

        mockMvc.perform(get("/parking/reservations/{id}", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) reservationId)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));
    }

    @Test
    void getReservation_notFound_returns404() throws Exception {
        mockMvc.perform(get("/parking/reservations/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReservationsByUser_returnsList() throws Exception {
        long space1 = createSpace();
        long space2 = createSpace();
        createReservation(space1, 7777L, 1L, START, END);
        createReservation(space2, 7777L, 2L, START, END);

        mockMvc.perform(get("/parking/reservations/user/{userId}", 7777L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void cancelReservation_returns200AndReleasesSpace() throws Exception {
        long spaceId = createSpace();
        long reservationId = createReservation(spaceId, 1L, 1L, START, END);

        mockMvc.perform(post("/parking/reservations/{id}/cancel", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        mockMvc.perform(get("/parking/spaces/{id}", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    void cancelReservation_alreadyCancelled_returns409() throws Exception {
        long spaceId = createSpace();
        long reservationId = createReservation(spaceId, 1L, 1L, START, END);

        mockMvc.perform(post("/parking/reservations/{id}/cancel", reservationId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/parking/reservations/{id}/cancel", reservationId))
                .andExpect(status().isConflict());
    }

    @Test
    void releaseReservation_returns200AndReleasesSpace() throws Exception {
        long spaceId = createSpace();
        long reservationId = createReservation(spaceId, 1L, 1L, START, END);

        mockMvc.perform(post("/parking/reservations/{id}/release", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        mockMvc.perform(get("/parking/spaces/{id}", spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    void releaseReservation_notConfirmed_returns409() throws Exception {
        long spaceId = createSpace();
        long reservationId = createReservation(spaceId, 1L, 1L, START, END);

        mockMvc.perform(post("/parking/reservations/{id}/cancel", reservationId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/parking/reservations/{id}/release", reservationId))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelReservation_notFound_returns404() throws Exception {
        mockMvc.perform(post("/parking/reservations/999999/cancel"))
                .andExpect(status().isNotFound());
    }

    private long createSpace() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "ownerId", 1L,
                "spaceNumber", "SP-" + java.util.UUID.randomUUID().toString().substring(0, 8),
                "location", "Level 1",
                "city", "Colombo",
                "zone", "Zone-A",
                "pricePerHour", 2.5));

        String response = mockMvc.perform(post("/parking/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createReservation(long spaceId, long userId, long vehicleId, String start, String end)
            throws Exception {
        String body = reservationBody(userId, vehicleId, spaceId, start, end);

        String response = mockMvc.perform(post("/parking/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private String reservationBody(long userId, long vehicleId, long spaceId, String start, String end) {
        return objectMapper.createObjectNode()
                .put("userId", userId)
                .put("vehicleId", vehicleId)
                .put("parkingSpaceId", spaceId)
                .put("startTime", start)
                .put("endTime", end)
                .toString();
    }
}
