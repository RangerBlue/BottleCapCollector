package com.km.bottlecapcollector.api.handler.exception;

public class AppForbiddenException extends RuntimeException
{
    public AppForbiddenException() {
    }

    public AppForbiddenException(String message) {
        super(message);
    }

    public AppForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public AppForbiddenException(Throwable cause) {
        super(cause);
    }

    public AppForbiddenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
