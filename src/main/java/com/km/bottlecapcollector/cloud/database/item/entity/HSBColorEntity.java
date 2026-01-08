package com.km.bottlecapcollector.cloud.database.item.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HSBColorEntity {

    private Float hue;

    private Float saturation;

    private Float brightness;
}
