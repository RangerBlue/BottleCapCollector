package com.km.bottlecapcollector.cloud.image.ml.vertex;

import com.km.bottlecapcollector.cloud.image.ml.api.EmbeddingException;

public class VertexEmbeddingException extends EmbeddingException {
    public VertexEmbeddingException() {
    }

    public VertexEmbeddingException(String message) {
        super(message);
    }
}
