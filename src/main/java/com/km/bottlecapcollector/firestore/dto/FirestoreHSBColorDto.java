package com.km.bottlecapcollector.firestore.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for transferring FirestoreHSBColor data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirestoreHSBColorDto {

    private Float hue;

    private Float saturation;

    private Float brightness;
}
