package com.km.bottlecapcollector.util;

import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.model.PictureItem;

public interface ItemFactory {
    CapItem getCapItem();
    PictureItem getPictureItem();
}
