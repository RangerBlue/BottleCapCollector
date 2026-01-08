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
public class ImageResponse {

    private String objectName;

    private String contentType;

    private Long sizeBytes;

    private String md5Hash;

    private Integer width;

    private Integer height;

    private Instant uploadedAt;

    private String signedUrl;
}
