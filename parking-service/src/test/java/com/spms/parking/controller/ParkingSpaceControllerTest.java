package com.spms.parking.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class ParkingSpaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createSpace_returns201() throws Exception {
        String body = """
                {"ownerId":1,"spaceNumber":"A-01","location":"Level 1, Near Gate A","city":"Colombo","zone":"Zone-A","pricePerHour":2.50}
                """;

        mockMvc.perform(post("/parking/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ownerId", is(1)))
                .andExpect(jsonPath("$.spaceNumber", is("A-01")))
                .andExpect(jsonPath("$.city", is("Colombo")))
                .andExpect(jsonPath("$.zone", is("Zone-A")))
                .andExpect(jsonPath("$.pricePerHour", is(2.5)))
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    void createSpace_invalidInput_returns400() throws Exception {
        String body = """
                {"ownerId":0,"spaceNumber":"","location":"","city":"","zone":"","pricePerHour":-1}
                """;

        mockMvc.perform(post("/parking/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("spaceNumber")));
    }

    @Test
    void getSpace_returns200() throws Exception {
        long id = createSpace("GET-0001", "Colombo", "Zone-A");

        mockMvc.perform(get("/parking/spaces/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    void getSpace_notFound_returns404() throws Exception {
        mockMvc.perform(get("/parking/spaces/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSpace_invalidId_returns400() throws Exception {
        mockMvc.perform(get("/parking/spaces/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSpace_malformedJson_returns400() throws Exception {
        String body = """
                {"ownerId":1,"spaceNumber":
                """;

        mockMvc.perform(post("/parking/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listAll_returnsArrayContainingCreatedSpace() throws Exception {
        createSpace("ALL-0001", "Colombo", "Zone-A");

        mockMvc.perform(get("/parking/spaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].spaceNumber", hasItem("ALL-0001")));
    }

    @Test
    void filterByCity_returnsMatchingSpaces() throws Exception {
        createSpace("CITY-0001", "CityAlpha", "Zone-A");
        createSpace("CITY-0002", "CityAlpha", "Zone-B");
        createSpace("CITY-0003", "OtherCity", "Zone-A");

        mockMvc.perform(get("/parking/spaces")
                        .param("city", "CityAlpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].spaceNumber", is("CITY-0001")))
                .andExpect(jsonPath("$[1].spaceNumber", is("CITY-0002")));
    }

    @Test
    void filterByZone_returnsMatchingSpaces() throws Exception {
        createSpace("ZONE-0001", "Colombo", "ZoneBeta");
        createSpace("ZONE-0002", "Colombo", "ZoneBeta");
        createSpace("ZONE-0003", "Colombo", "ZoneOther");

        mockMvc.perform(get("/parking/spaces")
                        .param("zone", "ZoneBeta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void filterByAvailable_returnsOnlyAvailable() throws Exception {
        long availableId = createSpace("AVL-0001", "Colombo", "ZoneGamma");
        long occupiedId = createSpace("AVL-0002", "Colombo", "ZoneGamma");
        updateStatus(occupiedId, "OCCUPIED");

        mockMvc.perform(get("/parking/spaces")
                        .param("zone", "ZoneGamma")
                        .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].spaceNumber", is("AVL-0001")));

        mockMvc.perform(get("/parking/spaces")
                        .param("zone", "ZoneGamma")
                        .param("available", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].spaceNumber", is("AVL-0002")));
    }

    @Test
    void filterByCityAndAvailable_returnsMatching() throws Exception {
        createSpace("CA-0001", "CityDelta", "Zone-A");
        createSpace("CA-0002", "CityDelta", "Zone-B");
        long maintenanceId = createSpace("CA-0003", "CityDelta", "Zone-C");
        updateStatus(maintenanceId, "MAINTENANCE");

        mockMvc.perform(get("/parking/spaces")
                        .param("city", "CityDelta")
                        .param("available", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].spaceNumber", is("CA-0001")))
                .andExpect(jsonPath("$[1].spaceNumber", is("CA-0002")));
    }

    @Test
    void filterByAvailable_invalidValue_returns400() throws Exception {
        mockMvc.perform(get("/parking/spaces")
                        .param("available", "not-a-bool"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSpace_returns200() throws Exception {
        long id = createSpace("UPD-0001", "Colombo", "Zone-A");

        String body = """
                {"city":"Galle","zone":"Zone-B","pricePerHour":3.75}
                """;

        mockMvc.perform(put("/parking/spaces/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city", is("Galle")))
                .andExpect(jsonPath("$.zone", is("Zone-B")))
                .andExpect(jsonPath("$.pricePerHour", is(3.75)))
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    void updateSpace_notFound_returns404() throws Exception {
        String body = """
                {"city":"Galle"}
                """;

        mockMvc.perform(put("/parking/spaces/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSpace_returns204() throws Exception {
        long id = createSpace("DEL-0001", "Colombo", "Zone-A");

        mockMvc.perform(delete("/parking/spaces/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteSpace_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/parking/spaces/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_returns200() throws Exception {
        long id = createSpace("STS-0001", "Colombo", "Zone-A");

        String body = """
                {"status":"OCCUPIED"}
                """;

        mockMvc.perform(put("/parking/spaces/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OCCUPIED")));
    }

    @Test
    void updateStatus_notFound_returns404() throws Exception {
        String body = """
                {"status":"MAINTENANCE"}
                """;

        mockMvc.perform(put("/parking/spaces/999999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_invalidStatus_returns400() throws Exception {
        long id = createSpace("STS-0002", "Colombo", "Zone-A");

        String body = """
                {"status":"UNKNOWN"}
                """;

        mockMvc.perform(put("/parking/spaces/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_missingStatus_returns400() throws Exception {
        long id = createSpace("STS-0003", "Colombo", "Zone-A");

        String body = "{}";

        mockMvc.perform(put("/parking/spaces/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private long createSpace(String spaceNumber, String city, String zone) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "ownerId", 1L,
                "spaceNumber", spaceNumber,
                "location", "Level 1",
                "city", city,
                "zone", zone,
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

    private void updateStatus(long id, String status) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("status", status));

        mockMvc.perform(put("/parking/spaces/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
