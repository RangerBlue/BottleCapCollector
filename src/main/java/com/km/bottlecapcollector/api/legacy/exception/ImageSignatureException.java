package com.km.bottlecapcollector.api.legacy.exception;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;

public class ImageSignatureException extends AppBadRequestException {
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
