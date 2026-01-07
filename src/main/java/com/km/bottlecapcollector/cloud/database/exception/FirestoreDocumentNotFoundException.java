package com.km.bottlecapcollector.cloud.database.exception;

import com.km.bottlecapcollector.api.handler.exception.AppResourceNotFoundException;

/**
 * Exception thrown when a Firestore document is not found.
 */
public class FirestoreDocumentNotFoundException extends AppResourceNotFoundException {

    public FirestoreDocumentNotFoundException(String documentId) {
        super("Document not found with id: " + documentId);
    }

    public FirestoreDocumentNotFoundException(String collection, String documentId) {
        super("Document not found in collection '" + collection + "' with id: " + documentId);
    }
}
