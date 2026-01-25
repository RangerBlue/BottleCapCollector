package com.km.bottlecapcollector.api.handler.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final int limit;
    private final int currentUsage;
    private final String limitType;

    public RateLimitExceededException(int limit, int currentUsage) {
        super(String.format("Daily identification limit exceeded. Used %d of %d allowed calls.", currentUsage, limit));
        this.limit = limit;
        this.currentUsage = currentUsage;
        this.limitType = "identification";
    }

    public RateLimitExceededException(String message, int limit, int currentUsage, String limitType) {
        super(message);
        this.limit = limit;
        this.currentUsage = currentUsage;
        this.limitType = limitType;
    }

    public static RateLimitExceededException itemLimitExceeded(int limit, long currentUsage) {
        return new RateLimitExceededException(
                String.format("Item limit exceeded. You have %d items, maximum allowed is %d.", currentUsage, limit),
                limit,
                (int) currentUsage,
                "items"
        );
    }

    public static RateLimitExceededException identificationLimitExceeded(int limit, int currentUsage) {
        return new RateLimitExceededException(limit, currentUsage);
    }
}
