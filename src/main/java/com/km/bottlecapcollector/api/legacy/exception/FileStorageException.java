package com.km.bottlecapcollector.api.legacy.exception;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;

public class FileStorageException extends AppBadRequestException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
