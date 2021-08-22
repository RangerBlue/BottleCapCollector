package com.km.bottlecapcollector.util;

import com.km.bottlecapcollector.model.BottleCap;

public class BottleCapPair {
    private BottleCap firstCap;
    private BottleCap secondCap;

    public BottleCapPair(BottleCap fistCap, BottleCap secondCap) {
        this.firstCap = fistCap;
        this.secondCap = secondCap;
    }

    public BottleCap getFirstCap() {
        return firstCap;
    }

    public BottleCap getSecondCap() {
        return secondCap;
    }

    @Override
    public String toString() {
        return "BottleCapPair{" +
                "firstCap=" + firstCap +
                ", secondCap=" + secondCap +
                '}';
    }
}
