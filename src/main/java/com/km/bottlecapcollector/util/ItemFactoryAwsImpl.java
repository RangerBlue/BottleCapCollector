package com.km.bottlecapcollector.util;

import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.model.PictureItem;
import com.km.bottlecapcollector.model.enums.CollectionItemType;
import com.km.bottlecapcollector.model.enums.ImageProviderType;
import com.km.bottlecapcollector.model.enums.SignatureType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "bcc.image.provider", havingValue = "s3")
public class ItemFactoryAwsImpl implements ItemFactory {
    @Override
    public CapItem getCapItem() {
        return (CapItem) new CollectionItemBuilder()
                .setCollectionItemType(CollectionItemType.CAP)
                .setSignatureType(SignatureType.OPEN_CV)
                .setImageProviderType(ImageProviderType.AWS)
                .build();
    }

    @Override
    public PictureItem getPictureItem() {
        return (PictureItem) new CollectionItemBuilder()
                .setCollectionItemType(CollectionItemType.PICTURE)
                .setSignatureType(SignatureType.OPEN_CV)
                .setImageProviderType(ImageProviderType.AWS)
                .build();
    }
}
