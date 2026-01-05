package com.km.bottlecapcollector.gcp.storage.api;

public class StorageException extends RuntimeException {
    public StorageException() {
    }

    public StorageException(String message) {
        super(message);
    }
}
