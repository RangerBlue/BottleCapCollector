package com.km.bottlecapcollector.cloud.image.identification.gemini;

import com.km.bottlecapcollector.cloud.image.identification.api.ImageIdentificationException;

public class GeminiImageIdentificationException extends ImageIdentificationException {

    public GeminiImageIdentificationException(String message) {
        super(message);
    }

    public GeminiImageIdentificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
