package com.km.bottlecapcollector.firestore.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Firestore embedded document representing Vertex AI embeddings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirestoreEmbedding {

    private List<Float> vector;

    private Integer dimensions;

    private String modelId;

    private String modelVersion;

    private Instant generatedAt;
}
