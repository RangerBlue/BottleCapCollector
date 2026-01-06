package com.km.bottlecapcollector.cloud.storage;

import com.km.bottlecapcollector.cloud.storage.api.StorageException;

public class CloudStorageException extends StorageException {
    public CloudStorageException() {
    }

    public CloudStorageException(String message) {
        super(message);
    }
}
