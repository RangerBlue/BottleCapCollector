package com.km.bottlecapcollector.cloud.storage.api;

public class StorageException extends RuntimeException {
    public StorageException() {
    }

    public StorageException(String message) {
        super(message);
    }
}
