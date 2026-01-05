package com.km.bottlecapcollector.firestore.exception;

/**
 * Exception thrown when a Firestore document is not found.
 */
public class FirestoreDocumentNotFoundException extends FirestoreException {

    public FirestoreDocumentNotFoundException(String documentId) {
        super("Document not found with id: " + documentId);
    }

    public FirestoreDocumentNotFoundException(String collection, String documentId) {
        super("Document not found in collection '" + collection + "' with id: " + documentId);
    }
}
