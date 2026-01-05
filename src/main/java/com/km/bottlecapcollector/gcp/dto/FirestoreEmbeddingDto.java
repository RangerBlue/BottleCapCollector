package com.km.bottlecapcollector.gcp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for transferring FirestoreEmbedding data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirestoreEmbeddingDto {

    private List<Float> vector;

    private Integer dimensions;

    private String modelId;

    private String modelVersion;

    private Instant generatedAt;
}
