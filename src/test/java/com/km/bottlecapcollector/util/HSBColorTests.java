package com.km.bottlecapcollector.util;
import com.km.bottlecapcollector.util.color.HSBColor;
import com.km.bottlecapcollector.util.color.HSBColorRange;
import com.km.bottlecapcollector.util.color.HSBColorService;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HSBColorTests {

    @BeforeClass
    public static void beforeClass(){
        HSBColorService.setMargin(0.1f);
    }

    @Test
    public void testParametersWithoutOverflow(){
        final HSBColor color = new HSBColor(0.5f, 0.4f, 0.3f);
        final HSBColorRange colorRange = HSBColorService.calculateRanges(color);
        assertEquals(0.4f, colorRange.getHueRange().getMoreThan(), 0.0001f);
        assertEquals(0.5f, colorRange.getHueRange().getPivotLessThan(), 0.0001f);
        assertEquals(0.5f, colorRange.getHueRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.6f, colorRange.getHueRange().getLessThan(), 0.0001f);

        assertEquals(0.3f, colorRange.getSaturationRange().getMoreThan(), 0.0001f);
        assertEquals(0.4f, colorRange.getSaturationRange().getPivotLessThan(), 0.0001f);
        assertEquals(0.4f, colorRange.getSaturationRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.5f, colorRange.getSaturationRange().getLessThan(), 0.0001f);

        assertEquals(0.2f, colorRange.getBrightnessRange().getMoreThan(), 0.0001f);
        assertEquals(0.3f, colorRange.getBrightnessRange().getPivotLessThan(), 0.0001f);
        assertEquals(0.3f, colorRange.getBrightnessRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.4f, colorRange.getBrightnessRange().getLessThan(), 0.0001f);
    }

    @Test
    public void testParametersWithOverflowTo0(){
        final HSBColor color = new HSBColor(0.05f, 0.3f, 0.4f);
        final HSBColorRange colorRange = HSBColorService.calculateRanges(color);
        assertEquals(0.95f, colorRange.getHueRange().getMoreThan(), 0.0001f);
        assertEquals(1f, colorRange.getHueRange().getPivotLessThan(), 0.0001f);
        assertEquals(0f, colorRange.getHueRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.15f, colorRange.getHueRange().getLessThan(), 0.0001f);

        assertEquals(0.2f, colorRange.getSaturationRange().getMoreThan(), 0.0001f);
        assertEquals(0.3f, colorRange.getSaturationRange().getPivotLessThan(), 0.0001f);
        assertEquals(0.3f, colorRange.getSaturationRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.4f, colorRange.getSaturationRange().getLessThan(), 0.0001f);

        assertEquals(0.3f, colorRange.getBrightnessRange().getMoreThan(), 0.0001f);
        assertEquals(0.4f, colorRange.getBrightnessRange().getPivotLessThan(), 0.0001f);
        assertEquals(0.4f, colorRange.getBrightnessRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.5f, colorRange.getBrightnessRange().getLessThan(), 0.0001f);
    }

    @Test
    public void testParametersWithOverflowTo1(){
        final HSBColor color = new HSBColor(0.95f, 0.2f, 0.8f);
        final HSBColorRange colorRange = HSBColorService.calculateRanges(color);

        assertEquals(0.85f, colorRange.getHueRange().getMoreThan(), 0.0001f);
        assertEquals(1f, colorRange.getHueRange().getPivotLessThan(), 0.0001f);
        assertEquals(0f, colorRange.getHueRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.05f, colorRange.getHueRange().getLessThan(), 0.0001f);

        assertEquals(0.1f, colorRange.getSaturationRange().getMoreThan(), 0.0001f);
        assertEquals(0.2f, colorRange.getSaturationRange().getPivotLessThan(), 0.0001f);
        assertEquals(0.2f, colorRange.getSaturationRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.3f, colorRange.getSaturationRange().getLessThan(), 0.0001f);

        assertEquals(0.7f, colorRange.getBrightnessRange().getMoreThan(), 0.0001f);
        assertEquals(0.8f, colorRange.getBrightnessRange().getPivotLessThan(), 0.0001f);
        assertEquals(0.8f, colorRange.getBrightnessRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.9f, colorRange.getBrightnessRange().getLessThan(), 0.0001f);
    }

    @Test
    public void testParametersWithOverflowEdgeCase(){
        final HSBColor color = new HSBColor(1f, 0f, 1f);
        final HSBColorRange colorRange = HSBColorService.calculateRanges(color);

        assertEquals(0.9f, colorRange.getHueRange().getMoreThan(), 0.0001f);
        assertEquals(1f, colorRange.getHueRange().getPivotLessThan(), 0.0001f);
        assertEquals(0f, colorRange.getHueRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.1f, colorRange.getHueRange().getLessThan(), 0.0001f);

        assertEquals(0.9f, colorRange.getSaturationRange().getMoreThan(), 0.0001f);
        assertEquals(1f, colorRange.getSaturationRange().getPivotLessThan(), 0.0001f);
        assertEquals(0f, colorRange.getSaturationRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.1f, colorRange.getSaturationRange().getLessThan(), 0.0001f);

        assertEquals(0.9f, colorRange.getBrightnessRange().getMoreThan(), 0.0001f);
        assertEquals(1f, colorRange.getBrightnessRange().getPivotLessThan(), 0.0001f);
        assertEquals(0f, colorRange.getBrightnessRange().getPivotMoreThan(), 0.0001f);
        assertEquals(0.1f, colorRange.getBrightnessRange().getLessThan(), 0.0001f);
    }
}
