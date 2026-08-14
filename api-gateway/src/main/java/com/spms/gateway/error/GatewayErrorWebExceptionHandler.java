package com.spms.gateway.error;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CancellationException;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ErrorAttributes errorAttributes;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public GatewayErrorWebExceptionHandler(ErrorAttributes errorAttributes, ServerCodecConfigurer serverCodecConfigurer) {
        this.errorAttributes = errorAttributes;
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted() || isDisconnectedClientError(ex)) {
            return Mono.error(ex);
        }

        errorAttributes.storeErrorInformation(ex, exchange);

        ServerRequest request = ServerRequest.create(exchange, serverCodecConfigurer.getReaders());
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(request,
                ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE,
                        ErrorAttributeOptions.Include.ERROR, ErrorAttributeOptions.Include.PATH));

        HttpStatusCode status = determineStatus(attributes, ex);
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = buildBody(attributes, status).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isDisconnectedClientError(Throwable ex) {
        return ex instanceof CancellationException || (ex instanceof IOException && isConnectionReset((IOException) ex));
    }

    private boolean isConnectionReset(IOException ex) {
        String message = ex.getMessage();
        return message != null && (message.contains("Connection reset by peer") || message.contains("Broken pipe"));
    }

    private HttpStatusCode determineStatus(Map<String, Object> attributes, Throwable ex) {
        Object status = attributes.get("status");
        if (status instanceof Integer code) {
            return HttpStatusCode.valueOf(code);
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String buildBody(Map<String, Object> attributes, HttpStatusCode status) {
        String path = asText(attributes.get("path"));
        String error = asText(attributes.get("error"));
        String message = asText(attributes.get("message"));
        if (message.isBlank() || "null".equals(message)) {
            message = reasonPhrase(status);
        }
        String timestamp = String.valueOf(attributes.getOrDefault("timestamp", System.currentTimeMillis()));
        return "{\"timestamp\":\"" + escape(timestamp)
                + "\",\"status\":" + status.value()
                + ",\"error\":\"" + escape(error)
                + "\",\"path\":\"" + escape(path)
                + "\",\"message\":\"" + escape(message) + "\"}";
    }

    private String reasonPhrase(HttpStatusCode status) {
        if (status instanceof HttpStatus httpStatus) {
            return httpStatus.getReasonPhrase();
        }
        return "";
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
