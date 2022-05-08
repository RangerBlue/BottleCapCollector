package com.km.bottlecapcollector.util;

import com.km.bottlecapcollector.model.AbstractSignature;
import com.km.bottlecapcollector.model.OpenCVImageSignature;
import com.km.bottlecapcollector.model.enums.SignatureType;

public class SignatureFactory {
    public static AbstractSignature getSignature(SignatureType type) {
        switch (type) {
            case OPEN_CV:
                return new OpenCVImageSignature();
            default:
                throw new UnsupportedOperationException();
        }
    }
}
