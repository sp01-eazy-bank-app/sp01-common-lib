package com.bank.exception.model;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final Error error;

    public <T extends Error> BusinessException(Error error) {
        super(error.getErrorMessage());
        this.error = error;
    }

    public <T extends Error> BusinessException(String message, Error error) {
        super(message);
        this.error = error;
    }
}
