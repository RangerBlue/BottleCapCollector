package com.km.bottlecapcollector.api;

import com.km.bottlecapcollector.api.handler.ControllerExceptionHandler;
import com.km.bottlecapcollector.api.handler.exception.AppBadRequestException;
import com.km.bottlecapcollector.api.handler.exception.AppResourceNotFoundException;
import com.km.bottlecapcollector.api.legacy.BottleCapController;
import com.km.bottlecapcollector.api.legacy.LegacyCollectionAdapter;
import com.km.bottlecapcollector.api.legacy.model.BottleCapDto;
import com.km.bottlecapcollector.api.legacy.model.BottleCapValidationResponseDto;
import com.km.bottlecapcollector.api.legacy.model.CapPictureDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BottleCapControllerTests {

    private MockMvc mvc;

    @Mock
    private LegacyCollectionAdapter legacyCollectionAdapter;

    @InjectMocks
    private BottleCapController bottleCapController;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(bottleCapController)
                .setControllerAdvice(new ControllerExceptionHandler())
                .build();
    }

    @Test
    void addBottleCapWithAdminRoleSuccess() throws Exception {
        String fileName = "captest1.jpg";
        String capName = "Beer";
        String description = "Good bear!";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(legacyCollectionAdapter.addCapItem(anyString(), anyString(), any())).willReturn("uuid-12345");

        this.mvc.perform(MockMvcRequestBuilders.multipart("/caps")
                        .file(file)
                        .param("name", capName)
                        .param("desc", description))
                .andExpect(status().is(201))
                .andExpect(jsonPath("$", is("uuid-12345")));
    }

    @Test
    void addBottleCapWithAdminRoleThrowsException() throws Exception {
        String fileName = "captest1.jpg";
        String capName = "Beer";
        String description = "Good bear!";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(legacyCollectionAdapter.addCapItem(anyString(), anyString(), any()))
                .willThrow(new AppBadRequestException("Upload failed"));

        this.mvc.perform(MockMvcRequestBuilders.multipart("/caps")
                        .file(file)
                        .param("name", capName)
                        .param("desc", description))
                .andExpect(status().is(500));
    }

    @Test
    void testValidateCap() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(legacyCollectionAdapter.validateCapItem(anyString(), any())).willReturn(new BottleCapValidationResponseDto(false,
                Arrays.asList("uuid-1", "uuid-2"), Arrays.asList("www.google.pl", "www.google.pl"),
                new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));

        this.mvc.perform(MockMvcRequestBuilders.multipart("/validateCap")
                        .file(file)
                        .param("name", fileName))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['similarCapsIDs']", hasSize(2)))
                .andExpect(jsonPath("$['similarCapsURLs']", hasSize(2)))
                .andExpect(jsonPath("$['similarityDistribution']", hasSize(10)))
                .andExpect(jsonPath("$['duplicate']", is(false)));
    }

    @Test
    void testWhatCapYouAre() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(legacyCollectionAdapter.validateWhatCapYouAre(anyString(), any())).willReturn(new BottleCapDto());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/whatCapAreYou")
                        .file(file)
                        .param("name", fileName))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['id']", nullValue()));
    }

    @Test
    void getBottleCapsSuccess() throws Exception {
        BottleCapDto cap = new BottleCapDto();
        cap.setName("cap1");
        cap.setUrl("location1");
        cap.setCreationDate(LocalTime.now().toString());
        BottleCapDto cap1 = new BottleCapDto();
        cap1.setName("cap2");
        cap1.setUrl("location2");
        cap1.setCreationDate(LocalTime.now().toString());
        List<BottleCapDto> allCaps = Arrays.asList(cap, cap1);
        given(legacyCollectionAdapter.getAllBottleCapsDto()).willReturn(allCaps);

        mvc.perform(get("/caps")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].url", is("location1")))
                .andExpect(jsonPath("$[0].name", is("cap1")))
                .andExpect(jsonPath("$[0].creationDate").isNotEmpty())
                .andExpect(jsonPath("$[0].description").isEmpty())
                .andExpect(jsonPath("$[1].url", is("location2")))
                .andExpect(jsonPath("$[1].name", is("cap2")))
                .andExpect(jsonPath("$[1].creationDate").isNotEmpty())
                .andExpect(jsonPath("$[1].description").isEmpty());
    }

    @Test
    void getLinksSuccess() throws Exception {
        CapPictureDto cap = new CapPictureDto();
        cap.setId("uuid-1");
        cap.setUrl("link");
        CapPictureDto cap1 = new CapPictureDto();
        cap1.setId("uuid-2");
        cap1.setUrl("link1");
        List<CapPictureDto> allCaps = Arrays.asList(cap, cap1);

        given(legacyCollectionAdapter.getAllBottleCapsLinks()).willReturn(allCaps);

        mvc.perform(get("/links")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("uuid-1")))
                .andExpect(jsonPath("$[0].url", is("link")))
                .andExpect(jsonPath("$[1].id", is("uuid-2")))
                .andExpect(jsonPath("$[1].url", is("link1")));
    }

    @Test
    void getBottleCapSuccess() throws Exception {
        BottleCapDto bottleCapDto = new BottleCapDto();
        bottleCapDto.setName("cap1");
        bottleCapDto.setUrl("");
        bottleCapDto.setCreationDate("2021-01-09T19:48:51.438");
        given(legacyCollectionAdapter.getCapItemDto(anyString())).willReturn(bottleCapDto);
        this.mvc.perform(get("/caps/uuid-1"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['url']").isEmpty())
                .andExpect(jsonPath("$['name']", is("cap1")))
                .andExpect(jsonPath("$['creationDate']").isNotEmpty())
                .andExpect(jsonPath("$['description']").isEmpty());
    }

    @Test
    void getBottleCapWrongIDException() throws Exception {
        String id = "uuid-1";
        given(legacyCollectionAdapter.getCapItemDto(id)).willThrow(new AppResourceNotFoundException(id));
        this.mvc.perform(get("/caps/uuid-1"))
                .andExpect(status().is(404));
    }

    @Test
    void updateBottleCapWithAdminRoleSuccess() throws Exception {
        this.mvc.perform(put("/caps/1")
                        .param("newName", "Beer")
                        .param("newDesc", "Good beer"))
                .andExpect(status().is(200));
    }

    @Test
    void updateBottleCapWrongIDException() throws Exception {
        String id = "uuid-1";
        String newName = "Beer";
        String newDesc = "Good beer";
        given(legacyCollectionAdapter.updateCapItemDto(id, newName, newDesc)).willThrow(new AppResourceNotFoundException(id));
        this.mvc.perform(put("/caps/uuid-1")
                        .param("newName", newName)
                        .param("newDesc", newDesc))
                .andExpect(status().is(404));
    }

    @Test
    void deleteBottleCapWithAdminRoleSuccess() throws Exception {
        this.mvc.perform(delete("/caps/1"))
                .andExpect(status().is(200));
    }

    @Test
    void deleteBottleCapNotFoundWithAdminRoleException() throws Exception {
        String capId = "uuid-12";
        doThrow(new AppResourceNotFoundException(capId)).when(legacyCollectionAdapter).removeCapItem(capId);
        this.mvc.perform(delete("/caps/" + capId))
                .andExpect(status().is(404));
    }
}
