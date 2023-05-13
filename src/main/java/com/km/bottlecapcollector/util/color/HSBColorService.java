package com.km.bottlecapcollector.util.color;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Slf4j
public class HSBColorService {

    private static float margin = 0;
    private static final float OVERFLOW_1 = 1f;
    private static final float OVERFLOW_0 = 0f;

    public static HSBColor calculateColor(MultipartFile file) {
        BufferedImage img;

        // read image file
        try {
            img = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // get image width and height
        int width = img.getWidth();
        int height = img.getHeight();

        // calculate the average color of all the pixels in the image
        long sumRed = 0, sumGreen = 0, sumBlue = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixelColor = new Color(img.getRGB(x, y));
                sumRed += pixelColor.getRed();
                sumGreen += pixelColor.getGreen();
                sumBlue += pixelColor.getBlue();
            }
        }
        int numPixels = width * height;
        int avgRed = (int) (sumRed / numPixels);
        int avgGreen = (int) (sumGreen / numPixels);
        int avgBlue = (int) (sumBlue / numPixels);

        float[] hsb = Color.RGBtoHSB(avgRed, avgGreen, avgBlue, null);

        float hue = hsb[0];
        float saturation = hsb[1];
        float brightness = hsb[2];

        log.info("Calculated color: Hue: {}, Saturation: {}, Brightness: {}", hue, saturation, brightness);

        return new HSBColor(hue, saturation, brightness);
    }

    public static HSBColorRange calculateRanges(HSBColor color) {
        return calculateRanges(color, margin);
    }

    private static HSBColorRange calculateRanges(HSBColor color, float margin) {
        float hue = color.getHue();
        float saturation = color.getSaturation();
        float brightness = color.getBrightness();
        log.info("Calculating {} for margin {}", color, margin);
        HSBColorRange colorRange = new HSBColorRange();
        colorRange.setHueRange(calculateWithOverflow(hue, margin));
        colorRange.setSaturationRange(calculateWithOverflow(saturation, margin));
        colorRange.setBrightnessRange(calculateWithOverflow(brightness, margin));

        return colorRange;
    }

    public static HSBColorRange calculateRangesExtended(HSBColor color) {
        return calculateRanges(color, margin * 2);
    }

    private static ColorRange calculateWithOverflow(float hsbParam, float margin) {
        ColorRange colorRange = new ColorRange();
        float hsbParamDiffLeft = hsbParam - margin;
        float hsbParamDiffRight = hsbParam + margin;
        if (hsbParamDiffLeft < 0) {
            colorRange.setMoreThan(OVERFLOW_1 + hsbParamDiffLeft);
            colorRange.setPivotLessThan(OVERFLOW_1);
            colorRange.setPivotMoreThan(OVERFLOW_0);
            colorRange.setLessThan(hsbParamDiffRight);
        } else if (hsbParamDiffRight > 1) {
            colorRange.setMoreThan(hsbParamDiffLeft);
            colorRange.setPivotLessThan(OVERFLOW_1);
            colorRange.setPivotMoreThan(OVERFLOW_0);
            colorRange.setLessThan(hsbParamDiffRight - OVERFLOW_1);
        } else {
            colorRange.setMoreThan(hsbParamDiffLeft);
            colorRange.setPivotLessThan(hsbParam);
            colorRange.setPivotMoreThan(hsbParam);
            colorRange.setLessThan(hsbParamDiffRight);
        }
        return colorRange;
    }

    public static void setMargin(float margin) {
        HSBColorService.margin = margin;
    }
}
