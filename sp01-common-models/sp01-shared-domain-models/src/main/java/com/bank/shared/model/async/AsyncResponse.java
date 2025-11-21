package com.bank.shared.model.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsyncResponse<T> {

    private T data;
    private ErrorResponse error;
}
