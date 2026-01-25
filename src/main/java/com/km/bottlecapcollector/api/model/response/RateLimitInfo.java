package com.km.bottlecapcollector.api.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Rate limit information for the current user")
public class RateLimitInfo {

    @Schema(description = "Maximum number of calls allowed per day. -1 indicates unlimited (ADMIN role)", example = "20")
    private int limit;

    @Schema(description = "Number of calls already used today", example = "5")
    private int used;

    @Schema(description = "Remaining calls for today. -1 indicates unlimited (ADMIN role)", example = "15")
    private int remaining;

    /**
     * Creates a RateLimitInfo for unlimited access (ADMIN role).
     */
    public static RateLimitInfo unlimited() {
        return RateLimitInfo.builder()
                .limit(-1)
                .used(0)
                .remaining(-1)
                .build();
    }

    /**
     * Creates a RateLimitInfo with the given usage stats.
     */
    public static RateLimitInfo of(int limit, int used) {
        return RateLimitInfo.builder()
                .limit(limit)
                .used(used)
                .remaining(limit - used)
                .build();
    }
}
