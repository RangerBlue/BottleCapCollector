package com.km.BottleCapCollector.exception;

public class DuplicateCapException extends RuntimeException {
    public DuplicateCapException(String message) {
        super(message);
    }

    public DuplicateCapException(String message, Throwable cause) {
        super(message, cause);
    }
}
