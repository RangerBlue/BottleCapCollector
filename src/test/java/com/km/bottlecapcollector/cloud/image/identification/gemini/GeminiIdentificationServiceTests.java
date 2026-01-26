package com.km.bottlecapcollector.cloud.image.identification.gemini;

import com.google.genai.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Method;

import com.km.bottlecapcollector.cloud.image.identification.api.ImageIdentification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiIdentificationServiceTests {

    @Mock
    private Client geminiClient;

    @Mock
    private MultipartFile multipartFile;

    private IdentificationService service;

    private static final String MODEL_ID = "gemini-2.0-flash";

    @BeforeEach
    void setUp() {
        service = new GeminiIdentificationService(geminiClient, MODEL_ID);
    }

    @Test
    void isAvailable_shouldReturnTrueWhenClientExists() {
        assertTrue(service.isAvailable());
    }

    @Test
    void isAvailable_shouldReturnFalseWhenClientIsNull() {
        IdentificationService nullService = new GeminiIdentificationService(null, MODEL_ID);
        assertFalse(nullService.isAvailable());
    }

    @Test
    void identifyItem_shouldThrowExceptionWhenServiceNotAvailable() {
        IdentificationService nullService = new GeminiIdentificationService(null, MODEL_ID);

        assertThrows(GeminiImageIdentificationException.class, () ->
                nullService.identifyItem(multipartFile));
    }

    @Test
    void identifyItem_shouldHandleFileReadError() throws IOException {
        // Given
        when(multipartFile.getBytes()).thenThrow(new IOException("File read error"));
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");

        // When / Then
        GeminiImageIdentificationException exception = assertThrows(
                GeminiImageIdentificationException.class,
                () -> service.identifyItem(multipartFile));

        assertTrue(exception.getMessage().contains("Failed to read file"));
    }

    @Test
    void parseResponse_shouldExtractAllFields() throws Exception {
        // Given
        String responseText = """
                PRIMARY_NAME: Heineken
                COMPANY: Heineken N.V.
                COUNTRY: Netherlands
                CATEGORY: Beer
                DESCRIPTION: Green bottle cap with red star logo and Heineken text
                SUGGESTED_TAGS: beer, green, heineken, dutch, lager
                CONFIDENCE: 0.95
                """;

        Method parseResponseMethod = GeminiIdentificationService.class
                .getDeclaredMethod("parseResponse", String.class);
        parseResponseMethod.setAccessible(true);

        // When
        ImageIdentification result = (ImageIdentification) parseResponseMethod.invoke(service, responseText);

        // Then
        assertNotNull(result);
        assertEquals("Heineken", result.getPrimaryName());
        assertEquals("Heineken N.V.", result.getCompany());
        assertEquals("Netherlands", result.getCountry());
        assertEquals("Beer", result.getCategory());
        assertEquals("Green bottle cap with red star logo and Heineken text", result.getDescription());
        assertEquals(5, result.getSuggestedTags().size());
        assertTrue(result.getSuggestedTags().contains("heineken"));
        assertTrue(result.getSuggestedTags().contains("dutch"));
        assertEquals(0.95, result.getConfidence(), 0.01);
    }

    @Test
    void parseResponse_shouldHandleUnknownValues() throws Exception {
        // Given
        String responseText = """
                PRIMARY_NAME: Unknown Cap
                COMPANY: Unknown
                COUNTRY: Unknown
                CATEGORY: Bottle Cap
                DESCRIPTION: Unidentified bottle cap
                SUGGESTED_TAGS: cap, unknown
                CONFIDENCE: 0.3
                """;

        Method parseResponseMethod = GeminiIdentificationService.class
                .getDeclaredMethod("parseResponse", String.class);
        parseResponseMethod.setAccessible(true);

        // When
        ImageIdentification result = (ImageIdentification) parseResponseMethod.invoke(service, responseText);

        // Then
        assertNotNull(result);
        assertEquals("Unknown Cap", result.getPrimaryName());
        assertEquals("Unknown", result.getCompany());
        assertEquals("Unknown", result.getCountry());
        assertEquals("Bottle Cap", result.getCategory());
        assertEquals(0.3, result.getConfidence(), 0.01);
    }
}
