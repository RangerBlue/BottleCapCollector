package com.km.bottlecapcollector.exception;

public class ImageSignatureException extends RuntimeException {
    public ImageSignatureException() {
    }

    public ImageSignatureException(String message) {
        super(message);
    }

    public ImageSignatureException(Throwable cause) {
        super(cause);
    }

    public ImageSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
