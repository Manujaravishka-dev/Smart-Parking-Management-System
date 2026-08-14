package com.spms.gateway.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webflux.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

class GatewayErrorWebExceptionHandlerTest {

    private final GatewayErrorWebExceptionHandler handler = new GatewayErrorWebExceptionHandler(
            new DefaultErrorAttributes(), ServerCodecConfigurer.create());

    @Test
    void handlesServiceUnavailable_as503Json() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/parking/spaces").build());

        handler.handle(exchange, new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable"))
                .block();

        assertEquals(503, exchange.getResponse().getStatusCode().value());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"status\":503"));
        assertTrue(body.contains("\"message\":\"Service Unavailable\""));
        assertTrue(body.contains("\"path\":\"/api/parking/spaces\""));
    }

    @Test
    void handlesNoRouteFound_as404Json() {
        MockServerWebExchange exchange = MockServerWebExchange
                .from(MockServerHttpRequest.get("/api/unknown").build());

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND, "No handler found"))
                .block();

        assertEquals(404, exchange.getResponse().getStatusCode().value());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"status\":404"));
        assertTrue(body.contains("\"path\":\"/api/unknown\""));
        assertTrue(body.contains("\"error\":\"Not Found\""));
    }
}
