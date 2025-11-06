package com.bank.shared.model.async;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AsyncResponse<T> {

    private T data;
    private ErrorResponse errorResponse;

    public static <T> AsyncResponse<T> success(T data) {
        AsyncResponse<T> response = new AsyncResponse<>();
        response.data = data;
        return response;
    }

    public static <T> AsyncResponse<T> failure(ErrorResponse error) {
        AsyncResponse<T> response = new AsyncResponse<>();
        response.errorResponse = error;
        return response;
    }
}
