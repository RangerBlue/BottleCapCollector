package com.km.bottlecapcollector.api.handler.exception;

public class AppBadRequestException extends RuntimeException {
    public AppBadRequestException() {
    }

    public AppBadRequestException(String message) {
        super(message);
    }

    public AppBadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppBadRequestException(Throwable cause) {
        super(cause);
    }

    public AppBadRequestException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
