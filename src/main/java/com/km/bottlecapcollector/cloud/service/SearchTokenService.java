package com.km.bottlecapcollector.cloud.service;

import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.ImageAnalysisMetadataEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for generating searchable tokens from CollectionItem fields.
 * Tokens are lowercase prefixes that enable efficient prefix search in Firestore
 * using array-contains queries.
 */
@Service
public class SearchTokenService {

    private static final int MIN_TOKEN_LENGTH = 2;
    private static final int MAX_TOKENS_PER_ITEM = 500;

    /**
     * Generates search tokens for a CollectionItem from:
     * - name
     * - customTags values
     * - Vision API label descriptions
     * - Vision API text words
     * - Vision API logo description
     *
     * @param item the collection item to generate tokens for
     * @return list of unique lowercase tokens
     */
    public List<String> generateTokens(ItemEntity item) {
        Set<String> tokens = new HashSet<>();

        // Extract from name
        if (item.getName() != null) {
            addTokensFromText(tokens, item.getName());
        }

        // Extract from customTags values
        if (item.getCustomTags() != null) {
            for (String value : item.getCustomTags().values()) {
                if (value != null) {
                    addTokensFromText(tokens, value);
                }
            }
        }

        // Extract from Vision metadata
        if (item.getVisionMetadata() != null) {
            extractVisionTokens(tokens, item.getVisionMetadata());
        }

        // Limit total tokens to prevent document size issues
        List<String> result = new ArrayList<>(tokens);
        if (result.size() > MAX_TOKENS_PER_ITEM) {
            return result.subList(0, MAX_TOKENS_PER_ITEM);
        }
        return result;
    }

    /**
     * Generates tokens from specific fields without requiring a full CollectionItem.
     * Useful for generating tokens during item creation before Vision metadata is available.
     */
    public List<String> generateTokens(String name, Map<String, String> customTags, ImageAnalysisMetadataEntity visionMetadata) {
        Set<String> tokens = new HashSet<>();

        if (name != null) {
            addTokensFromText(tokens, name);
        }

        if (customTags != null) {
            for (String value : customTags.values()) {
                if (value != null) {
                    addTokensFromText(tokens, value);
                }
            }
        }

        if (visionMetadata != null) {
            extractVisionTokens(tokens, visionMetadata);
        }

        List<String> result = new ArrayList<>(tokens);
        if (result.size() > MAX_TOKENS_PER_ITEM) {
            return result.subList(0, MAX_TOKENS_PER_ITEM);
        }
        return result;
    }

    private void extractVisionTokens(Set<String> tokens, ImageAnalysisMetadataEntity metadata) {
        // Vision labels
        if (metadata.getImageLabels() != null) {
            for (ImageAnalysisMetadataEntity.FirestoreImageLabel imageLabel : metadata.getImageLabels()) {
                if (imageLabel.getDescription() != null) {
                    addTokensFromText(tokens, imageLabel.getDescription());
                }
            }
        }

        // Vision text words
        if (metadata.getTextAnnotation() != null) {
            ImageAnalysisMetadataEntity.FirestoreImageText textAnnotation = metadata.getTextAnnotation();
            if (textAnnotation.getWords() != null) {
                for (String word : textAnnotation.getWords()) {
                    if (word != null) {
                        addTokensFromText(tokens, word);
                    }
                }
            }
        }

        // Vision logo description
        if (metadata.getLogoAnnotation() != null && metadata.getLogoAnnotation().getDescription() != null) {
            addTokensFromText(tokens, metadata.getLogoAnnotation().getDescription());
        }
    }

    /**
     * Adds prefix tokens from a text string.
     * Splits by whitespace and generates prefixes for each word.
     */
    private void addTokensFromText(Set<String> tokens, String text) {
        String normalized = text.toLowerCase().trim();
        String[] words = normalized.split("\\s+");

        for (String word : words) {
            // Clean word from non-alphanumeric characters at boundaries
            word = word.replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
            if (word.length() >= MIN_TOKEN_LENGTH) {
                addPrefixes(tokens, word);
            }
        }
    }

    /**
     * Adds all prefixes of a word to the token set.
     * For "beer" adds: "be", "bee", "beer"
     */
    private void addPrefixes(Set<String> tokens, String word) {
        for (int i = MIN_TOKEN_LENGTH; i <= word.length(); i++) {
            tokens.add(word.substring(0, i));
        }
    }
}
