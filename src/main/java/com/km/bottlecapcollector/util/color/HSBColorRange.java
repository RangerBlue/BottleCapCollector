package com.km.bottlecapcollector.util.color;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HSBColorRange {
    private ColorRange hueRange;
    private ColorRange saturationRange;
    private ColorRange brightnessRange;
}
