package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.api.handler.exception.RateLimitExceededException;
import com.km.bottlecapcollector.api.model.response.RateLimitInfo;
import com.km.bottlecapcollector.cloud.database.user.entity.UserEntity;
import com.km.bottlecapcollector.cloud.database.user.repository.UserEntityRepository;
import com.km.bottlecapcollector.property.AppProperties;
import com.km.bottlecapcollector.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTests {

    private static final String USER_ID = "user-123";
    private static final int DEFAULT_LIMIT = 20;

    @Mock
    private UserEntityRepository userRepository;

    @Mock
    private AppProperties appProperties;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(userRepository, appProperties);
    }

    @Nested
    class AdminUserTests {

        @Test
        void checkAndIncrementIdentificationUsage_shouldReturnUnlimitedForAdmin() {
            // Given
            UserEntity adminUser = UserEntity.builder()
                    .id(USER_ID)
                    .role(Role.ADMIN)
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(adminUser));

            // When
            RateLimitInfo result = rateLimitService.checkAndIncrementIdentificationUsage(USER_ID);

            // Then
            assertNotNull(result);
            assertEquals(-1, result.getLimit());
            assertEquals(0, result.getUsed());
            assertEquals(-1, result.getRemaining());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class RegularUserTests {

        @Test
        void checkAndIncrementIdentificationUsage_shouldIncrementUsageForFirstCall() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(USER_ID)
                    .role(Role.USER)
                    .identificationUsageCount(0)
                    .lastIdentificationResetDate(Instant.now())
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(appProperties.getMaxIdentificationsPerDay()).thenReturn(DEFAULT_LIMIT);

            // When
            RateLimitInfo result = rateLimitService.checkAndIncrementIdentificationUsage(USER_ID);

            // Then
            assertNotNull(result);
            assertEquals(DEFAULT_LIMIT, result.getLimit());
            assertEquals(1, result.getUsed());
            assertEquals(DEFAULT_LIMIT - 1, result.getRemaining());

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals(1, userCaptor.getValue().getIdentificationUsageCount());
        }

        @Test
        void checkAndIncrementIdentificationUsage_shouldIncrementExistingUsage() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(USER_ID)
                    .role(Role.USER)
                    .identificationUsageCount(5)
                    .lastIdentificationResetDate(Instant.now())
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(appProperties.getMaxIdentificationsPerDay()).thenReturn(DEFAULT_LIMIT);

            // When
            RateLimitInfo result = rateLimitService.checkAndIncrementIdentificationUsage(USER_ID);

            // Then
            assertEquals(DEFAULT_LIMIT, result.getLimit());
            assertEquals(6, result.getUsed());
            assertEquals(DEFAULT_LIMIT - 6, result.getRemaining());
        }

        @Test
        void checkAndIncrementIdentificationUsage_shouldThrowWhenLimitExceeded() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(USER_ID)
                    .role(Role.USER)
                    .identificationUsageCount(DEFAULT_LIMIT)
                    .lastIdentificationResetDate(Instant.now())
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(appProperties.getMaxIdentificationsPerDay()).thenReturn(DEFAULT_LIMIT);

            // When / Then
            RateLimitExceededException exception = assertThrows(
                    RateLimitExceededException.class,
                    () -> rateLimitService.checkAndIncrementIdentificationUsage(USER_ID)
            );

            assertEquals(DEFAULT_LIMIT, exception.getLimit());
            assertEquals(DEFAULT_LIMIT, exception.getCurrentUsage());
            verify(userRepository, never()).save(any());
        }

        @Test
        void checkAndIncrementIdentificationUsage_shouldResetCounterOnNewDay() {
            // Given
            Instant yesterday = LocalDate.now().minusDays(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant();
            UserEntity user = UserEntity.builder()
                    .id(USER_ID)
                    .role(Role.USER)
                    .identificationUsageCount(15)
                    .lastIdentificationResetDate(yesterday)
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(appProperties.getMaxIdentificationsPerDay()).thenReturn(DEFAULT_LIMIT);

            // When
            RateLimitInfo result = rateLimitService.checkAndIncrementIdentificationUsage(USER_ID);

            // Then
            assertEquals(1, result.getUsed());
            assertEquals(DEFAULT_LIMIT - 1, result.getRemaining());

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals(1, userCaptor.getValue().getIdentificationUsageCount());
        }

        @Test
        void checkAndIncrementIdentificationUsage_shouldResetCounterWhenNoPreviousResetDate() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(USER_ID)
                    .role(Role.USER)
                    .identificationUsageCount(10)
                    .lastIdentificationResetDate(null)
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(appProperties.getMaxIdentificationsPerDay()).thenReturn(DEFAULT_LIMIT);

            // When
            RateLimitInfo result = rateLimitService.checkAndIncrementIdentificationUsage(USER_ID);

            // Then
            assertEquals(1, result.getUsed());

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals(1, userCaptor.getValue().getIdentificationUsageCount());
            assertNotNull(userCaptor.getValue().getLastIdentificationResetDate());
        }

        @Test
        void checkAndIncrementIdentificationUsage_shouldHandleNullUsageCount() {
            // Given
            UserEntity user = UserEntity.builder()
                    .id(USER_ID)
                    .role(Role.USER)
                    .identificationUsageCount(null)
                    .lastIdentificationResetDate(Instant.now())
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(appProperties.getMaxIdentificationsPerDay()).thenReturn(DEFAULT_LIMIT);

            // When
            RateLimitInfo result = rateLimitService.checkAndIncrementIdentificationUsage(USER_ID);

            // Then
            assertEquals(1, result.getUsed());
        }
    }

    @Nested
    class PerUserLimitOverrideTests {

        @Test
        void checkAndIncrementIdentificationUsage_shouldUsePerUserLimitWhenSet() {
            // Given
            int perUserLimit = 50;
            UserEntity user = UserEntity.builder()
                    .id(USER_ID)
                    .role(Role.USER)
                    .identificationUsageCount(0)
                    .lastIdentificationResetDate(Instant.now())
                    .maxIdentificationsPerDay(perUserLimit)
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // When
            RateLimitInfo result = rateLimitService.checkAndIncrementIdentificationUsage(USER_ID);

            // Then
            assertEquals(perUserLimit, result.getLimit());
            assertEquals(1, result.getUsed());
            assertEquals(perUserLimit - 1, result.getRemaining());
        }

        @Test
        void checkAndIncrementIdentificationUsage_shouldAllowHigherUsageWithPerUserLimit() {
            // Given
            int perUserLimit = 100;
            UserEntity user = UserEntity.builder()
                    .id(USER_ID)
                    .role(Role.USER)
                    .identificationUsageCount(25)
                    .lastIdentificationResetDate(Instant.now())
                    .maxIdentificationsPerDay(perUserLimit)
                    .build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // When
            RateLimitInfo result = rateLimitService.checkAndIncrementIdentificationUsage(USER_ID);

            // Then
            assertEquals(perUserLimit, result.getLimit());
            assertEquals(26, result.getUsed());
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void checkAndIncrementIdentificationUsage_shouldThrowWhenUserNotFound() {
            // Given
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // When / Then
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> rateLimitService.checkAndIncrementIdentificationUsage(USER_ID)
            );

            assertEquals("User not found: " + USER_ID, exception.getMessage());
        }
    }
}
