package com.km.bottlecapcollector.cloud.image.identification.api;

public class ImageIdentificationException extends RuntimeException {
    public ImageIdentificationException() {
    }

    public ImageIdentificationException(String message) {
        super(message);
    }


    public ImageIdentificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
