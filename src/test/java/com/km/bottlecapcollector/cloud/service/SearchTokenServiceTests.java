package com.km.bottlecapcollector.cloud.service;

import com.km.bottlecapcollector.cloud.database.item.entity.ImageAnalysisMetadataEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchTokenServiceTests {

    private SearchTokenService searchTokenService;

    @BeforeEach
    void setUp() {
        searchTokenService = new SearchTokenService();
    }

    @Nested
    class GenerateTokensFromItemTests {

        @Test
        void generateTokens_shouldGeneratePrefixesFromName() {
            // Given
            ItemEntity item = ItemEntity.builder()
                    .name("Beer")
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then - should generate "be", "bee", "beer"
            assertTrue(tokens.contains("be"));
            assertTrue(tokens.contains("bee"));
            assertTrue(tokens.contains("beer"));
        }

        @Test
        void generateTokens_shouldHandleMultiWordName() {
            // Given
            ItemEntity item = ItemEntity.builder()
                    .name("Coca Cola")
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.contains("co"));
            assertTrue(tokens.contains("coc"));
            assertTrue(tokens.contains("coca"));
            assertTrue(tokens.contains("col"));
            assertTrue(tokens.contains("cola"));
        }

        @Test
        void generateTokens_shouldConvertToLowercase() {
            // Given
            ItemEntity item = ItemEntity.builder()
                    .name("PEPSI")
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.contains("pe"));
            assertTrue(tokens.contains("pep"));
            assertTrue(tokens.contains("peps"));
            assertTrue(tokens.contains("pepsi"));
            assertFalse(tokens.contains("PEPSI"));
            assertFalse(tokens.contains("PE"));
        }

        @Test
        void generateTokens_shouldExtractFromCustomTags() {
            // Given
            Map<String, String> customTags = new HashMap<>();
            customTags.put("brand", "Heineken");
            customTags.put("country", "Netherlands");

            ItemEntity item = ItemEntity.builder()
                    .name("Beer Cap")
                    .customTags(customTags)
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.contains("heineken"));
            assertTrue(tokens.contains("hei"));
            assertTrue(tokens.contains("netherlands"));
            assertTrue(tokens.contains("net"));
        }

        @Test
        void generateTokens_shouldHandleNullName() {
            // Given
            ItemEntity item = ItemEntity.builder()
                    .name(null)
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.isEmpty());
        }

        @Test
        void generateTokens_shouldHandleNullCustomTags() {
            // Given
            ItemEntity item = ItemEntity.builder()
                    .name("Test")
                    .customTags(null)
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.contains("test"));
            assertTrue(tokens.contains("te"));
        }

        @Test
        void generateTokens_shouldSkipShortWords() {
            // Given
            ItemEntity item = ItemEntity.builder()
                    .name("A B CD")
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            // Only "CD" should generate tokens (length >= 2)
            assertTrue(tokens.contains("cd"));
            assertEquals(1, tokens.size());
        }

        @Test
        void generateTokens_shouldExtractFromVisionLabels() {
            // Given
            ImageAnalysisMetadataEntity.FirestoreImageLabel label = new ImageAnalysisMetadataEntity.FirestoreImageLabel();
            label.setDescription("Bottle cap");

            ImageAnalysisMetadataEntity metadata = ImageAnalysisMetadataEntity.builder()
                    .imageLabels(List.of(label))
                    .build();

            ItemEntity item = ItemEntity.builder()
                    .name("Test")
                    .visionMetadata(metadata)
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.contains("bottle"));
            assertTrue(tokens.contains("cap"));
        }

        @Test
        void generateTokens_shouldExtractFromVisionTextAnnotation() {
            // Given
            ImageAnalysisMetadataEntity.FirestoreImageText textAnnotation =
                    ImageAnalysisMetadataEntity.FirestoreImageText.builder()
                            .words(List.of("Premium", "Lager"))
                            .build();

            ImageAnalysisMetadataEntity metadata = ImageAnalysisMetadataEntity.builder()
                    .textAnnotation(textAnnotation)
                    .build();

            ItemEntity item = ItemEntity.builder()
                    .name("Test")
                    .visionMetadata(metadata)
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.contains("premium"));
            assertTrue(tokens.contains("lager"));
        }

        @Test
        void generateTokens_shouldExtractFromLogoAnnotation() {
            // Given
            ImageAnalysisMetadataEntity.FirestoreImageLogo logoAnnotation =
                    ImageAnalysisMetadataEntity.FirestoreImageLogo.builder()
                            .description("Budweiser")
                            .build();

            ImageAnalysisMetadataEntity metadata = ImageAnalysisMetadataEntity.builder()
                    .logoAnnotation(logoAnnotation)
                    .build();

            ItemEntity item = ItemEntity.builder()
                    .name("Test")
                    .visionMetadata(metadata)
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.contains("budweiser"));
            assertTrue(tokens.contains("bud"));
        }

        @Test
        void generateTokens_shouldRemoveNonAlphanumericBoundaries() {
            // Given
            ItemEntity item = ItemEntity.builder()
                    .name("(Test) -Sample-")
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.contains("test"));
            assertTrue(tokens.contains("sample"));
        }

        @Test
        void generateTokens_shouldGenerateUniqueTokens() {
            // Given
            Map<String, String> customTags = new HashMap<>();
            customTags.put("brand", "Beer");

            ItemEntity item = ItemEntity.builder()
                    .name("Beer")
                    .customTags(customTags)
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then - should not have duplicates
            long beerCount = tokens.stream().filter(t -> t.equals("beer")).count();
            assertEquals(1, beerCount);
        }

        @Test
        void generateTokens_shouldLimitTokenCount() {
            // Given - create item with many tags that would generate > 500 tokens
            StringBuilder longName = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longName.append("Word").append(i).append(" ");
            }

            ItemEntity item = ItemEntity.builder()
                    .name(longName.toString())
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertTrue(tokens.size() <= 500);
        }
    }

    @Nested
    class GenerateTokensFromFieldsTests {

        @Test
        void generateTokens_shouldWorkWithFieldsDirectly() {
            // Given
            String name = "Test Item";
            Map<String, String> customTags = Map.of("type", "beer");

            // When
            List<String> tokens = searchTokenService.generateTokens(name, customTags, null);

            // Then
            assertTrue(tokens.contains("test"));
            assertTrue(tokens.contains("item"));
            assertTrue(tokens.contains("beer"));
        }

        @Test
        void generateTokens_shouldHandleAllNullFields() {
            // When
            List<String> tokens = searchTokenService.generateTokens(null, null, null);

            // Then
            assertTrue(tokens.isEmpty());
        }

        @Test
        void generateTokens_shouldHandleEmptyCustomTags() {
            // When
            List<String> tokens = searchTokenService.generateTokens("Test", Map.of(), null);

            // Then
            assertTrue(tokens.contains("test"));
        }

        @Test
        void generateTokens_shouldHandleCustomTagsWithNullValues() {
            // Given
            Map<String, String> customTags = new HashMap<>();
            customTags.put("brand", null);
            customTags.put("type", "soda");

            // When
            List<String> tokens = searchTokenService.generateTokens("Test", customTags, null);

            // Then
            assertTrue(tokens.contains("soda"));
            assertTrue(tokens.contains("test"));
        }
    }

    @Nested
    class PrefixGenerationTests {

        @Test
        void generateTokens_shouldGenerateCorrectPrefixesForShortWord() {
            // Given
            ItemEntity item = ItemEntity.builder()
                    .name("ab")
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertEquals(1, tokens.size());
            assertTrue(tokens.contains("ab"));
        }

        @Test
        void generateTokens_shouldGenerateCorrectPrefixesForLongerWord() {
            // Given
            ItemEntity item = ItemEntity.builder()
                    .name("abcde")
                    .build();

            // When
            List<String> tokens = searchTokenService.generateTokens(item);

            // Then
            assertEquals(4, tokens.size()); // "ab", "abc", "abcd", "abcde"
            assertTrue(tokens.contains("ab"));
            assertTrue(tokens.contains("abc"));
            assertTrue(tokens.contains("abcd"));
            assertTrue(tokens.contains("abcde"));
        }
    }
}
