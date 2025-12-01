package com.bank.models.shared.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ApiResponse<T> {

    private final T data;
    private final String message;
    private final Instant timestamp;

    @JsonCreator
    public ApiResponse(@JsonProperty("data") T data, @JsonProperty("message") String message,
                        @JsonProperty("timestamp") Instant timestamp) {
        this.data = data;
        this.message = message;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(data, message, Instant.now());
    }
}
