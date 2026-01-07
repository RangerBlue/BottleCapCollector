package com.km.bottlecapcollector.api.legacy.exception;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;

public class DuplicateCapException extends AppBadRequestException {
    public DuplicateCapException(String message) {
        super(message);
    }

    public DuplicateCapException(String message, Throwable cause) {
        super(message, cause);
    }
}
