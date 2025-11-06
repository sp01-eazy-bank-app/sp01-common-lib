package com.bank.exception.model;

import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {

    private final Error error;

    public <T extends Error> TechnicalException(Error error) {
        super(error.getErrorMessage());
        this.error = error;
    }
}
