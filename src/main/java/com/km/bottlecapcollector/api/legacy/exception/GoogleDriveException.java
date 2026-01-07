package com.km.bottlecapcollector.api.legacy.exception;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;

public class GoogleDriveException extends AppBadRequestException {
    public GoogleDriveException() {
    }

    public GoogleDriveException(String message) {
        super(message);
    }

    public GoogleDriveException(String message, Throwable cause) {
        super(message, cause);
    }

    public GoogleDriveException(Throwable cause) {
        super(cause);
    }

    public GoogleDriveException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
