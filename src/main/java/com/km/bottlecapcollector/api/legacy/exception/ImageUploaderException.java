package com.km.bottlecapcollector.api.legacy.exception;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;

public class ImageUploaderException extends AppBadRequestException {
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
