package com.km.bottlecapcollector.api.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedCollectionResponse {

    private String collectionKey;

    private String collectionName;

    private String ownerUserId;

    private String ownerName;

    private Instant sharedAt;
}
