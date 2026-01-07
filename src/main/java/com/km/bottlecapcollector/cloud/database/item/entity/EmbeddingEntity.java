package com.km.bottlecapcollector.cloud.database.item.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingEntity {
    private List<Float> vector;
    private Integer dimensions;
    private String modelId;
    private String modelVersion;
    private Instant generatedAt;
}
