package com.km.bottlecapcollector.firestore.exception;

/**
 * Exception thrown when Firestore operations fail.
 */
public class FirestoreException extends RuntimeException {

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

    public FirestoreException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
