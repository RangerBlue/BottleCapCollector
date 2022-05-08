package com.km.bottlecapcollector.util;

import com.km.bottlecapcollector.model.*;
import com.km.bottlecapcollector.model.enums.CollectionItemType;
import com.km.bottlecapcollector.model.enums.ImageProviderType;
import com.km.bottlecapcollector.model.enums.PictureItem;
import com.km.bottlecapcollector.model.enums.SignatureType;

public class CollectionItemBuilder {
    CollectionItemType collectionItemType;
    ImageProviderType imageProviderType;
    SignatureType signatureType;


    public CollectionItemBuilder setCollectionItemType(CollectionItemType collectionItemType) {
        this.collectionItemType = collectionItemType;
        return this;
    }

    public CollectionItemBuilder setImageProviderType(ImageProviderType imageProviderType) {
        this.imageProviderType = imageProviderType;
        return this;
    }

    public CollectionItemBuilder setSignatureType(SignatureType signatureType) {
        this.signatureType = signatureType;
        return this;
    }

    public CollectionItem build() {
        CollectionItem item;

        if (collectionItemType == CollectionItemType.CAP && imageProviderType == ImageProviderType.GOOGLE && signatureType == SignatureType.OPEN_CV) {
            item = new CapItem();
            AbstractImage image = new RoundOpenCVGoogleImage();
            AbstractImageProvider abstractImageProvider = ImageProviderFactory.getSignature(imageProviderType);
            AbstractSignature openCVImageSignature = SignatureFactory.getSignature(signatureType);
            image.setProvider(abstractImageProvider);
            image.setSignature(openCVImageSignature);
            item.setImage(image);
        } else if (collectionItemType == CollectionItemType.PICTURE && imageProviderType == ImageProviderType.GOOGLE && signatureType == SignatureType.OPEN_CV) {
            item = new PictureItem();
            AbstractImage image = new RoundOpenCVGoogleImage();
            AbstractImageProvider abstractImageProvider = ImageProviderFactory.getSignature(imageProviderType);
            AbstractSignature openCVImageSignature = SignatureFactory.getSignature(signatureType);
            image.setProvider(abstractImageProvider);
            image.setSignature(openCVImageSignature);
            item.setImage(image);
        } else {
            throw new UnsupportedOperationException();
        }
        return item;
    }
}
