package com.km.bottlecapcollector.cloud.image.identification.gemini;

import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.km.bottlecapcollector.cloud.image.identification.api.ImageIdentification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GeminiIdentificationService implements IdentificationService {

    private final Client geminiClient;
    private final String modelId;

    private static final String IDENTIFICATION_PROMPT = """
            Analyze this image and identify the item shown.

            Use this hint to help narrow down your identification. Provide your response in the following format:

            PRIMARY_NAME: [The most specific name for this item, e.g., "Heineken" or "Coca-Cola"]
            COMPANY: [The company that produces this item, e.g., brewery for beer caps like "Heineken N.V.", "Anheuser-Busch InBev", or manufacturer for other items, for example Nintendo for video games. Use "Unknown" if not identifiable]
            COUNTRY: [The country of origin where the company is based or where the item is produced, e.g., "Netherlands", "United States", "Germany". Use "Unknown" if not identifiable]
            CATEGORY: [The category of the item, e.g., "Beer", "Soda", "Video game"]
            DESCRIPTION: [A brief description of the item, including any visible text, colors, or distinctive features]
            SUGGESTED_TAGS: [Comma-separated tags that would help categorize this item, e.g., "beer, green, heineken, dutch, crown cap"]
            CONFIDENCE: [Your confidence level from 0.0 to 1.0]

            Be specific and accurate. If you can identify the brand, include it in the primary name.
            """;

    public GeminiIdentificationService(Client geminiClient, @Qualifier("geminiModelId") String modelId) {
        this.geminiClient = geminiClient;
        this.modelId = modelId;
    }

    @Override
    public boolean isAvailable() {
        return geminiClient != null;
    }

    @Override
    public ImageIdentification identifyItem(MultipartFile file) {
        if (!isAvailable()) {
            throw new GeminiImageIdentificationException("Gemini service is not available");
        }

        log.info("Identifying item from file: {}", file.getOriginalFilename());

        try {
            byte[] imageBytes = file.getBytes();
            String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

            Blob imageBlob = Blob.builder()
                    .mimeType(mimeType)
                    .data(imageBytes)
                    .build();

            Content content = Content.builder()
                    .role("user")
                    .parts(List.of(
                            Part.builder().text(IDENTIFICATION_PROMPT).build(),
                            Part.builder().inlineData(imageBlob).build()
                    ))
                    .build();

            GenerateContentResponse response = geminiClient.models.generateContent(modelId, content, null);
            String responseText = response.text();

            log.debug("Gemini raw response: {}", responseText);

            ImageIdentification identification = parseResponse(responseText);
            identification.setRawResponse(responseText);

            log.info("Item identified: {} (confidence: {})", identification.getPrimaryName(), identification.getConfidence());
            return identification;

        } catch (IOException e) {
            log.error("Failed to read file for identification", e);
            throw new GeminiImageIdentificationException("Failed to read file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to identify item with Gemini", e);
            throw new GeminiImageIdentificationException("Failed to identify item: " + e.getMessage(), e);
        }
    }

    private ImageIdentification parseResponse(String responseText) {
        ImageIdentification.ImageIdentificationBuilder builder = ImageIdentification.builder();

        builder.primaryName(extractField(responseText, "PRIMARY_NAME"));
        builder.company(extractField(responseText, "COMPANY"));
        builder.country(extractField(responseText, "COUNTRY"));
        builder.category(extractField(responseText, "CATEGORY"));
        builder.description(extractField(responseText, "DESCRIPTION"));
        builder.suggestedTags(extractTags(responseText));
        builder.confidence(extractConfidence(responseText));

        return builder.build();
    }

    private String extractField(String text, String fieldName) {
        Pattern pattern = Pattern.compile(fieldName + ":\\s*(.+?)(?=\\n[A-Z_]+:|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private List<String> extractTags(String text) {
        String tagsStr = extractField(text, "SUGGESTED_TAGS");
        if (tagsStr == null || tagsStr.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(tagsStr.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private Double extractConfidence(String text) {
        String confidenceStr = extractField(text, "CONFIDENCE");
        if (confidenceStr == null) {
            return null;
        }
        try {
            return Double.parseDouble(confidenceStr.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse confidence: {}", confidenceStr);
            return null;
        }
    }
}
