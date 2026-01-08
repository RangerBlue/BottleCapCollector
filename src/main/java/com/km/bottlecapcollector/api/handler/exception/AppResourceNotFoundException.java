package com.km.bottlecapcollector.api.handler.exception;

public class AppResourceNotFoundException extends RuntimeException {
    public AppResourceNotFoundException() {
    }

    public AppResourceNotFoundException(String message) {
        super(message);
    }

    public AppResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppResourceNotFoundException(Throwable cause) {
        super(cause);
    }

    public AppResourceNotFoundException(String message, Throwable cause, boolean enableSuppression,
                                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
