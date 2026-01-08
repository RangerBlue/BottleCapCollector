package com.km.bottlecapcollector.cloud.database.exception;

import com.km.bottlecapcollector.api.handler.exception.AppResourceNotFoundException;

/**
 * Exception thrown when a Firestore document is not found.
 */
public class FirestoreDocumentNotFoundException extends AppResourceNotFoundException {

    public FirestoreDocumentNotFoundException(String documentId) {
        super("Resource not found with id: " + documentId);
    }

    public FirestoreDocumentNotFoundException(String collection, String documentId) {
        super("Resource not found in collection '" + collection + "' with id: " + documentId);
    }
}
