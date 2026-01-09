package com.km.bottlecapcollector.color;

import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;
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

    private HSBColorService() {
    }

    public static HSBColor calculateColor(MultipartFile file) {
        BufferedImage img;

        // read image file
        try {
            img = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new AppBadRequestException(e);
        }

        // get image width and height
        int width = img.getWidth();
        int height = img.getHeight();

        // calculate the average color of all the pixels in the image
        long sumRed = 0;
        long sumGreen = 0;
        long sumBlue = 0;
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

    /**
     * Calculates similarity score between two HSB colors (0.0 = different, 1.0 = identical).
     */
    public static Double calculateHSBSimilarity(HSBColor color1, HSBColor color2) {
        if (color2 == null) {
            return 0.0;
        }

        double hueDiff = Math.abs(color1.getHue() - color2.getHue());
        double satDiff = Math.abs(color1.getSaturation() - color2.getSaturation());
        double briDiff = Math.abs(color1.getBrightness() - color2.getBrightness());

        // Hue is circular, so take the shorter distance
        if (hueDiff > 0.5) {
            hueDiff = 1.0 - hueDiff;
        }

        // Weighted average (hue is most important for color perception)
        double distance = (hueDiff * 0.5) + (satDiff * 0.25) + (briDiff * 0.25);

        // Convert distance to similarity (0 distance = 1.0 similarity)
        return Math.max(0.0, 1.0 - (distance * 2));
    }

    public static void setMargin(float margin) {
        HSBColorService.margin = margin;
    }
}
