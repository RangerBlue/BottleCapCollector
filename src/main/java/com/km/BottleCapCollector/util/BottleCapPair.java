package com.km.BottleCapCollector.util;

import com.km.BottleCapCollector.model.BottleCap;

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
