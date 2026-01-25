package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.handler.exception.RateLimitExceededException;
import com.km.bottlecapcollector.api.model.response.RateLimitInfo;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.cloud.database.user.repository.UserEntityRepository;
import com.km.bottlecapcollector.property.AppProperties;
import com.km.bottlecapcollector.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Service for managing rate limits on API usage.
 * Currently handles daily limits for item identification calls.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private final UserEntityRepository userRepository;
    private final AppProperties appProperties;

    /**
     * Checks if the user can make an identification call and increments the usage counter.
     * Throws RateLimitExceededException if the daily limit has been reached.
     * ADMIN users bypass rate limiting entirely.
     *
     * @param userId the user ID
     * @return RateLimitInfo with current usage stats (after incrementing)
     * @throws RateLimitExceededException if the user has exceeded their daily limit
     */
    public RateLimitInfo checkAndIncrementIdentificationUsage(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        // ADMIN users have unlimited access
        if (Role.ADMIN.equals(user.getRole())) {
            log.trace("User {} is ADMIN, bypassing rate limit", userId);
            return RateLimitInfo.unlimited();
        }

        int limit = getEffectiveLimit(user);
        LocalDate today = LocalDate.now();

        // Lazy reset: if it's a new day, reset the counter
        LocalDate lastResetDate = user.getLastIdentificationResetDate() != null
                ? user.getLastIdentificationResetDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                : null;
        if (lastResetDate == null || !lastResetDate.equals(today)) {
            log.info("Resetting identification usage for user {} (new day)", userId);
            user.setIdentificationUsageCount(0);
            user.setLastIdentificationResetDate(Instant.now());
        }

        int currentUsage = user.getIdentificationUsageCount() != null ? user.getIdentificationUsageCount() : 0;

        // Check if limit exceeded BEFORE incrementing
        if (currentUsage >= limit) {
            log.warn("User {} has exceeded daily identification limit ({}/{})", userId, currentUsage, limit);
            throw new RateLimitExceededException(limit, currentUsage);
        }

        // Increment usage
        int newUsage = currentUsage + 1;
        user.setIdentificationUsageCount(newUsage);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        log.debug("User {} identification usage: {}/{}", userId, newUsage, limit);

        return RateLimitInfo.of(limit, newUsage);
    }

    /**
     * Gets the effective daily limit for a user, considering per-user override.
     */
    private int getEffectiveLimit(UserEntity user) {
        if (user.getMaxIdentificationsPerDay() != null) {
            return user.getMaxIdentificationsPerDay();
        }
        return appProperties.getMaxIdentificationsPerDay();
    }
}
