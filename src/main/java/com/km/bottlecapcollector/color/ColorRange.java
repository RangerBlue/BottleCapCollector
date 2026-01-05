package com.km.bottlecapcollector.color;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ColorRange {
    private float moreThan;
    private float pivotLessThan;
    private float pivotMoreThan;
    private float lessThan;
}
