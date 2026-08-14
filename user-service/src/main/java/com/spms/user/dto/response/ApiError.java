package com.spms.user.dto.response;

import java.time.LocalDateTime;

public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String path,
        String message) {

    public static ApiError of(int status, String error, String path, String message) {
        return new ApiError(LocalDateTime.now(), status, error, path, message);
    }
}
