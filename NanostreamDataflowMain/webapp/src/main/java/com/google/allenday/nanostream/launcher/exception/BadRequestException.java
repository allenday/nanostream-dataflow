package com.google.allenday.nanostream.launcher.exception;

public class BadRequestException extends RuntimeException {

    private String error;

    public BadRequestException(String error, String message) {
        super(message);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}

