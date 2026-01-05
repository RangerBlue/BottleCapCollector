package com.km.bottlecapcollector.gcp.storage;

import com.km.bottlecapcollector.gcp.storage.api.StorageException;

public class CloudStorageException extends StorageException {
    public CloudStorageException() {
    }

    public CloudStorageException(String message) {
        super(message);
    }
}
