package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.dto.BottleCapDto;
import com.km.bottlecapcollector.dto.BottleCapValidationResponseDto;
import com.km.bottlecapcollector.dto.CapPictureDto;
import com.km.bottlecapcollector.exception.CapNotFoundException;
import com.km.bottlecapcollector.exception.ControllerExceptionHandler;
import com.km.bottlecapcollector.exception.GoogleDriveException;
import com.km.bottlecapcollector.service.BottleCapService;
import com.km.bottlecapcollector.util.ItemFactory;
import com.km.bottlecapcollector.util.ItemFactoryImpl;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
    private BottleCapService bottleCapService;

    @InjectMocks
    private BottleCapController bottleCapController;

    ItemFactory itemFactory = new ItemFactoryImpl();

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
        given(bottleCapService.addCapItem(capName, description, file)).willReturn(12L);

        this.mvc.perform(MockMvcRequestBuilders.multipart("/caps")
                        .file(file)
                        .param("name", capName)
                        .param("desc", description))
                .andExpect(status().is(201))
                .andExpect(jsonPath("$", is(12)));
    }

    @Test
    void addBottleCapWithAdminRoleThrowsException() throws Exception {
        String fileName = "captest1.jpg";
        String capName = "Beer";
        String description = "Good bear!";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(bottleCapService.addCapItem(capName, description, file)).willThrow(new GoogleDriveException());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/caps")
                        .file(file)
                        .param("name", capName)
                        .param("desc", description))
                .andExpect(status().is(400));
    }

    @Test
    void testValidateCap() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(bottleCapService.validateCapItem(fileName, file)).willReturn(new BottleCapValidationResponseDto(false,
                Arrays.asList(1L, 2L), Arrays.asList("www.google.pl", "www.google.pl"),
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
        given(bottleCapService.validateWhatCapYouAre(fileName, file)).willReturn(new BottleCapDto());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/whatCapAreYou")
                        .file(file)
                        .param("name", fileName))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['id']", is(0)));
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
        given(bottleCapService.getAllBottleCapsDto()).willReturn(allCaps);

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
        cap.setId(1);
        cap.setUrl("link");
        CapPictureDto cap1 = new CapPictureDto();
        cap1.setId(2);
        cap1.setUrl("link1");
        List<CapPictureDto> allCaps = Arrays.asList(cap, cap1);

        given(bottleCapService.getAllBottleCapsLinks()).willReturn(allCaps);

        mvc.perform(get("/links")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].url", is("link")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].url", is("link1")));
    }

    @Test
    void getBottleCapSuccess() throws Exception {
        BottleCapDto bottleCapDto = new BottleCapDto();
        bottleCapDto.setName("cap1");
        bottleCapDto.setUrl("");
        bottleCapDto.setCreationDate("2021-01-09T19:48:51.438");
        given(bottleCapService.getCapItemDto(anyLong())).willReturn(bottleCapDto);
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['url']").isEmpty())
                .andExpect(jsonPath("$['name']", is("cap1")))
                .andExpect(jsonPath("$['creationDate']").isNotEmpty())
                .andExpect(jsonPath("$['description']").isEmpty());
    }

    @Test
    void getBottleCapWrongIDException() throws Exception {
        long id = 1;
        given(bottleCapService.getCapItemDto(id)).willThrow(new CapNotFoundException(id));
        this.mvc.perform(get("/caps/1"))
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
        long id = 1;
        String newName = "Beer";
        String newDesc = "Good beer";
        given(bottleCapService.updateCapItemDto(id, newName, newDesc)).willThrow(new CapNotFoundException(id));
        this.mvc.perform(put("/caps/1")
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
        long capId = 12L;
        doThrow(new CapNotFoundException(capId)).when(bottleCapService).removeCapItem(capId);
        this.mvc.perform(delete("/caps/" + capId))
                .andExpect(status().is(404));
    }
}
