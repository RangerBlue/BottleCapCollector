package com.km.bottlecapcollector.api.legacy.exception;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;

public class MyFileNotFoundException extends AppBadRequestException {
    public MyFileNotFoundException(String message) {
        super(message);
    }

    public MyFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
