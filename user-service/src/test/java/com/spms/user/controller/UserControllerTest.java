package com.spms.user.controller;

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
        "spring.datasource.url=jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_returns201() throws Exception {
        String body = """
                {"name":"Alice Smith","email":"alice@example.com","password":"password123","phone":"555-1234","role":"DRIVER"}
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name", is("Alice Smith")))
                .andExpect(jsonPath("$.email", is("alice@example.com")))
                .andExpect(jsonPath("$.role", is("DRIVER")));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        String body = """
                {"name":"Alice Smith","email":"dup@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    void register_invalidInput_returns400() throws Exception {
        String body = """
                {"name":"","email":"not-an-email","password":"123","phone":"12345","role":"DRIVER"}
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    void login_success_returns200() throws Exception {
        register("login@example.com", "password123");

        String body = """
                {"email":"login@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("success")))
                .andExpect(jsonPath("$.user.email", is("login@example.com")));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        register("badpw@example.com", "password123");

        String body = """
                {"email":"badpw@example.com","password":"wrongpass"}
                """;

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        String body = """
                {"email":"ghost@example.com","password":"password123"}
                """;

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUser_returns200() throws Exception {
        long id = register("get@example.com", "password123");

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.email", is("get@example.com")));
    }

    @Test
    void getUser_notFound_returns404() throws Exception {
        mockMvc.perform(get("/users/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void updateUser_returns200() throws Exception {
        long id = register("update@example.com", "password123");

        String body = """
                {"name":"Alice Updated","phone":"999-9999","role":"OWNER"}
                """;

        mockMvc.perform(put("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice Updated")))
                .andExpect(jsonPath("$.phone", is("999-9999")))
                .andExpect(jsonPath("$.role", is("OWNER")));
    }

    @Test
    void updateUser_conflictEmail_returns409() throws Exception {
        long firstId = register("first@example.com", "password123");
        register("second@example.com", "password123");

        String body = """
                {"email":"second@example.com"}
                """;

        mockMvc.perform(put("/users/{id}", firstId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void getBookings_returnsEmptyList() throws Exception {
        long id = register("book@example.com", "password123");

        mockMvc.perform(get("/users/{id}/bookings", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getUser_invalidId_returns400() throws Exception {
        mockMvc.perform(get("/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid value for parameter")));
    }

    @Test
    void register_invalidRole_returns400() throws Exception {
        String body = """
                {"name":"Bad Role","email":"badrole@example.com","password":"password123","role":"NINJA"}
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_malformedJson_returns400() throws Exception {
        String body = """
                {"name":"Broken JSON",
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    private long register(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Alice Smith",
                "email", email,
                "password", password));

        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
