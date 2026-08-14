package com.spms.payment.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spms.payment.client.ReservationClient;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:paymentdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationClient reservationClient;

    @BeforeEach
    void setUp() {
        when(reservationClient.exists(anyLong())).thenReturn(true);
    }

    @Test
    void processPayment_returns201WithSuccessAndMaskedCard() throws Exception {
        String body = paymentBody(1001L, 1L, "500", "CARD", "4111111111111111");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.reservationId", is(1001)))
                .andExpect(jsonPath("$.amount", is(500)))
                .andExpect(jsonPath("$.paymentMethod", is("CARD")))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.maskedCardNumber", is("************1111")));
    }

    @Test
    void processPayment_cash_returns201WithoutCard() throws Exception {
        String body = paymentBody(1002L, 1L, "500", "CASH", null);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.maskedCardNumber").doesNotExist());
    }

    @Test
    void processPayment_declinedCard_returns201WithFailedStatus() throws Exception {
        String body = paymentBody(1003L, 1L, "500", "CARD", "4000000000000002");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.maskedCardNumber", is("************0002")));
    }

    @Test
    void processPayment_invalidCard_returns400() throws Exception {
        String body = paymentBody(1004L, 1L, "500", "CARD", "4111111111111112");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cardNumber")));
    }

    @Test
    void processPayment_missingCardForCardMethod_returns400() throws Exception {
        String body = paymentBody(1005L, 1L, "500", "CARD", null);

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cardNumber")));
    }

    @Test
    void processPayment_missingFields_returns400() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("reservationId")));
    }

    @Test
    void processPayment_reservationNotFound_returns404() throws Exception {
        when(reservationClient.exists(anyLong())).thenReturn(false);
        String body = paymentBody(1006L, 1L, "500", "CARD", "4111111111111111");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Reservation not found")));
    }

    @Test
    void processPayment_duplicatePayment_returns409() throws Exception {
        String body = paymentBody(1007L, 1L, "500", "CARD", "4111111111111111");

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void getPayment_returns200() throws Exception {
        long paymentId = processPayment(1008L, 1L, "500", "CARD", "4111111111111111");

        mockMvc.perform(get("/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) paymentId)))
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    void getPayment_notFound_returns404() throws Exception {
        mockMvc.perform(get("/payments/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentsByReservation_returnsList() throws Exception {
        processPayment(9101L, 1L, "500", "CARD", "4000000000000002");
        processPayment(9102L, 1L, "500", "CARD", "4111111111111111");

        mockMvc.perform(get("/payments/reservation/{reservationId}", 9101L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getPaymentsByUser_returnsList() throws Exception {
        processPayment(9201L, 7777L, "500", "CARD", "4000000000000002");
        processPayment(9202L, 7777L, "500", "CARD", "4111111111111111");

        mockMvc.perform(get("/payments/user/{userId}", 7777L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getReceipt_returns200ForSuccessfulPayment() throws Exception {
        long paymentId = processPayment(1009L, 1L, "500", "CARD", "4111111111111111");

        mockMvc.perform(get("/payments/{id}/receipt", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId", is((int) paymentId)))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.reservationId", is(1009)))
                .andExpect(jsonPath("$.amount", is(500.0)))
                .andExpect(jsonPath("$.paymentStatus", is("SUCCESS")));
    }

    @Test
    void getReceipt_failedPayment_returns404() throws Exception {
        long paymentId = processPayment(1010L, 1L, "500", "CARD", "4000000000000002");

        mockMvc.perform(get("/payments/{id}/receipt", paymentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Receipt")));
    }

    @Test
    void getReceipt_notFound_returns404() throws Exception {
        mockMvc.perform(get("/payments/999999/receipt"))
                .andExpect(status().isNotFound());
    }

    private long processPayment(Long reservationId, Long userId, String amount, String method, String cardNumber)
            throws Exception {
        String body = paymentBody(reservationId, userId, amount, method, cardNumber);

        String response = mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private String paymentBody(Long reservationId, Long userId, String amount, String method, String cardNumber) {
        ObjectNode node = objectMapper.createObjectNode()
                .put("reservationId", reservationId)
                .put("userId", userId)
                .put("amount", amount)
                .put("paymentMethod", method);
        if (cardNumber != null) {
            node.put("cardNumber", cardNumber);
        }
        return node.toString();
    }
}
