package com.km.bottlecapcollector.util;

import com.km.bottlecapcollector.model.CapItem;

public class BottleCapPair {
    private CapItem firstCap;
    private CapItem secondCap;

    public BottleCapPair(CapItem fistCap, CapItem secondCap) {
        this.firstCap = fistCap;
        this.secondCap = secondCap;
    }

    public CapItem getFirstCap() {
        return firstCap;
    }

    public CapItem getSecondCap() {
        return secondCap;
    }

    @Override
    public String toString() {
        return "CapItem{" +
                "firstCap=" + firstCap +
                ", secondCap=" + secondCap +
                '}';
    }
}
