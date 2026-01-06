package com.km.bottlecapcollector.api.legacy.model;

import com.km.bottlecapcollector.opencv.ComparisonMethod;
import lombok.Data;

@Data
public class ComparisonRangeDto {
    private ComparisonMethod methodName;
    private double minValue;
    private double maxValue;
}
