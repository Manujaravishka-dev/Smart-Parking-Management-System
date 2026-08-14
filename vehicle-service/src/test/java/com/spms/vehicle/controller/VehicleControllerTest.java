package com.spms.vehicle.controller;

import static org.hamcrest.Matchers.containsString;
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
        "spring.datasource.url=jdbc:h2:mem:vehicledb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_returns201() throws Exception {
        String body = """
                {"userId":1,"vehicleNumber":"ABC-1234","vehicleType":"CAR","brand":"Toyota","model":"Camry"}
                """;

        mockMvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.vehicleNumber", is("ABC-1234")))
                .andExpect(jsonPath("$.vehicleType", is("CAR")))
                .andExpect(jsonPath("$.status", is("OUTSIDE")));
    }

    @Test
    void register_duplicateVehicleNumber_returns409() throws Exception {
        String body = """
                {"userId":1,"vehicleNumber":"DUP-0001","vehicleType":"CAR","brand":"Toyota","model":"Camry"}
                """;

        mockMvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    void register_invalidInput_returns400() throws Exception {
        String body = """
                {"userId":-1,"vehicleNumber":"","vehicleType":"CAR","brand":"","model":""}
                """;

        mockMvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("vehicleNumber")));
    }

    @Test
    void getVehicle_returns200() throws Exception {
        long id = register("GET-0001");

        mockMvc.perform(get("/vehicles/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.vehicleNumber", is("GET-0001")))
                .andExpect(jsonPath("$.status", is("OUTSIDE")));
    }

    @Test
    void getVehicle_notFound_returns404() throws Exception {
        mockMvc.perform(get("/vehicles/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void getVehiclesByUser_returnsList() throws Exception {
        registerForUser(777L, "USER-0001");
        registerForUser(777L, "USER-0002");
        registerForUser(2L, "OTHER-001");

        mockMvc.perform(get("/vehicles/user/{userId}", 777L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].vehicleNumber", is("USER-0001")))
                .andExpect(jsonPath("$[1].vehicleNumber", is("USER-0002")));
    }

    @Test
    void updateVehicle_returns200() throws Exception {
        long id = register("UPD-0001");

        String body = """
                {"vehicleType":"TRUCK","brand":"Volvo","model":"FH16"}
                """;

        mockMvc.perform(put("/vehicles/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType", is("TRUCK")))
                .andExpect(jsonPath("$.brand", is("Volvo")))
                .andExpect(jsonPath("$.model", is("FH16")))
                .andExpect(jsonPath("$.status", is("OUTSIDE")));
    }

    @Test
    void updateVehicle_notFound_returns404() throws Exception {
        String body = """
                {"brand":"Volvo"}
                """;

        mockMvc.perform(put("/vehicles/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteVehicle_returns204() throws Exception {
        long id = register("DEL-0001");

        mockMvc.perform(delete("/vehicles/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteVehicle_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/vehicles/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void entry_success_returns200AndInside() throws Exception {
        long id = register("ENT-0001");

        mockMvc.perform(post("/vehicles/{id}/entry", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INSIDE")))
                .andExpect(jsonPath("$.entryTime").exists());
    }

    @Test
    void entry_alreadyInside_returns409() throws Exception {
        long id = register("ENT-0002");

        mockMvc.perform(post("/vehicles/{id}/entry", id))
                .andExpect(status().isOk());

        mockMvc.perform(post("/vehicles/{id}/entry", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already inside")));
    }

    @Test
    void entry_notFound_returns404() throws Exception {
        mockMvc.perform(post("/vehicles/999999/entry"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exit_success_returns200AndOutside() throws Exception {
        long id = register("EXT-0001");

        mockMvc.perform(post("/vehicles/{id}/entry", id))
                .andExpect(status().isOk());

        mockMvc.perform(post("/vehicles/{id}/exit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OUTSIDE")))
                .andExpect(jsonPath("$.exitTime").exists());
    }

    @Test
    void exit_notInside_returns409() throws Exception {
        long id = register("EXT-0002");

        mockMvc.perform(post("/vehicles/{id}/exit", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("not currently inside")));
    }

    @Test
    void exit_notFound_returns404() throws Exception {
        mockMvc.perform(post("/vehicles/999999/exit"))
                .andExpect(status().isNotFound());
    }

    private long register(String vehicleNumber) throws Exception {
        return registerForUser(1L, vehicleNumber);
    }

    private long registerForUser(long userId, String vehicleNumber) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "userId", userId,
                "vehicleNumber", vehicleNumber,
                "vehicleType", "CAR",
                "brand", "Toyota",
                "model", "Camry"));

        String response = mockMvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
