package com.km.bottlecapcollector.util;

import com.km.bottlecapcollector.model.AbstractImageProvider;
import com.km.bottlecapcollector.model.AwsS3Provider;
import com.km.bottlecapcollector.model.GoogleDriveProvider;
import com.km.bottlecapcollector.model.enums.ImageProviderType;

public class ImageProviderFactory {
    public static AbstractImageProvider getSignature(ImageProviderType type) {
        switch (type) {
            case GOOGLE:
                return new GoogleDriveProvider();
            case AWS:
                return new AwsS3Provider();
            default:
                throw new UnsupportedOperationException();
        }
    }
}
