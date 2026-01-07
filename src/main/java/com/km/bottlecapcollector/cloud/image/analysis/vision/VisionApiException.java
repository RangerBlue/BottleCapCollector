package com.km.bottlecapcollector.cloud.image.analysis.vision;

import com.km.bottlecapcollector.cloud.image.analysis.api.ImageAnalysisException;

public class VisionApiException extends ImageAnalysisException {
    public VisionApiException() {
    }

    public VisionApiException(String message) {
        super(message);
    }
}
