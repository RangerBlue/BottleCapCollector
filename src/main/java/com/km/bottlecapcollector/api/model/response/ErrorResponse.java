package com.km.bottlecapcollector.api.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "Timestamp when the error occurred", example = "2026-01-25T12:00:00Z")
    private Instant timestamp;

    @Schema(description = "HTTP status code", example = "429")
    private int status;

    @Schema(description = "Error details. For rate limit errors contains: message, limitType, limit, used, remaining")
    private Object error;

    public static ErrorResponse create(int status, String error) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .build();
    }

}
