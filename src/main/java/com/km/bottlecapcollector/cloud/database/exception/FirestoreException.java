package com.km.bottlecapcollector.cloud.database.exception;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;

public class FirestoreException extends AppBadRequestException {

    public FirestoreException() {
    }

    public FirestoreException(String message) {
        super(message);
    }

    public FirestoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public FirestoreException(Throwable cause) {
        super(cause);
    }

    public FirestoreException(String message, Throwable cause, boolean enableSuppression,
                              boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
