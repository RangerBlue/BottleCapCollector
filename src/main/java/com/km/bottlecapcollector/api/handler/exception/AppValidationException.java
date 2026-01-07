package com.km.bottlecapcollector.api.handler.exception;

public class AppValidationException extends RuntimeException {
    public AppValidationException() {
    }

    public AppValidationException(String message) {
        super(message);
    }

    public AppValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppValidationException(Throwable cause) {
        super(cause);
    }

    public AppValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
