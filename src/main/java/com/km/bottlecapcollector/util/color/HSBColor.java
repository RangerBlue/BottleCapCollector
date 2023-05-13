package com.km.bottlecapcollector.util.color;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HSBColor {
    float hue;
    float saturation;
    float brightness;

    public HSBColor(float hue, float saturation, float brightness) {
        this.setHue(hue);
        this.setSaturation(saturation);
        this.setBrightness(brightness);
    }

    public void setHue(float hue) {
        validateValue(hue);
        this.hue = hue;
    }

    public void setSaturation(float saturation) {
        validateValue(saturation);
        this.saturation = saturation;
    }

    public void setBrightness(float brightness) {
        validateValue(brightness);
        this.brightness = brightness;
    }

    private void validateValue(float value){
        if(value < 0 || value > 1){
            throw new  IllegalArgumentException("Value must be between 0 and 1 inclusively");
        }
    }
}
