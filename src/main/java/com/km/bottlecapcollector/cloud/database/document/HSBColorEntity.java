package com.km.bottlecapcollector.cloud.database.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Firestore embedded document representing HSB (Hue, Saturation, Brightness) color values.
 * Used for pre-filtering bottle caps before applying embedding-based similarity search.
 * Values are stored as floats in range 0.0 to 1.0.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HSBColorEntity {

    private Float hue;

    private Float saturation;

    private Float brightness;
}
