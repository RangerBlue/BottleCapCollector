package com.km.bottlecapcollector.exception;

public class ImageUploaderException extends RuntimeException {
    public ImageUploaderException() {
    }

    public ImageUploaderException(String message) {
        super(message);
    }

    public ImageUploaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageUploaderException(Throwable cause) {
        super(cause);
    }
}
