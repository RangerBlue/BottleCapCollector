package com.km.bottlecapcollector.util;

import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.model.enums.CollectionItemType;
import com.km.bottlecapcollector.model.enums.ImageProviderType;
import com.km.bottlecapcollector.model.enums.PictureItem;
import com.km.bottlecapcollector.model.enums.SignatureType;

public class ItemFactoryImpl implements ItemFactory{

    @Override
    public CapItem getCapItem() {
        return (CapItem) new CollectionItemBuilder()
                .setCollectionItemType(CollectionItemType.CAP)
                .setSignatureType(SignatureType.OPEN_CV)
                .setImageProviderType(ImageProviderType.GOOGLE)
                .build();
    }

    @Override
    public PictureItem getPictureItem() {
        return (PictureItem) new CollectionItemBuilder()
                .setCollectionItemType(CollectionItemType.PICTURE)
                .setSignatureType(SignatureType.OPEN_CV)
                .setImageProviderType(ImageProviderType.GOOGLE)
                .build();
    }
}
