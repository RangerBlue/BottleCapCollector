package com.km.bottlecapcollector.cloud.image.identification.gemini;

import com.google.genai.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
