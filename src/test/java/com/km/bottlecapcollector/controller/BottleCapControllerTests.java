package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.dto.BottleCapCatalogDto;
import com.km.bottlecapcollector.dto.BottleCapDto;
import com.km.bottlecapcollector.dto.BottleCapPictureDto;
import com.km.bottlecapcollector.dto.BottleCapValidationResponseDto;
import com.km.bottlecapcollector.exception.CapNotFoundException;
import com.km.bottlecapcollector.exception.GoogleDriveException;
import com.km.bottlecapcollector.google.GoogleDriveService;
import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.service.BottleCapService;
import com.km.bottlecapcollector.util.ItemFactory;
import com.km.bottlecapcollector.util.ItemFactoryImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = BottleCapController.class)
public class BottleCapControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BottleCapService bottleCapService;

    @MockBean
    private GoogleDriveService googleDriveService;

    @MockBean
    private DataSource dataSource;

    ItemFactory itemFactory = new ItemFactoryImpl();


    @Test
    @WithMockUser(roles = "ADMIN")
    public void addBottleCapWithAdminRoleSuccess() throws Exception {
        String fileName = "captest1.jpg";
        String capName = "Beer";
        String description = "Good bear!";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data" .getBytes());
        given(bottleCapService.addCapItem(capName, description, file)).willReturn(12L);


        this.mvc.perform(MockMvcRequestBuilders.multipart("/caps")
                        .file(file)
                        .param("name", capName)
                        .param("desc", description))
                .andExpect(status().is(201))
                .andExpect(jsonPath("$", is(12)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void addBottleCapWithAdminRoleThrowsException() throws Exception {
        String fileName = "captest1.jpg";
        String capName = "Beer";
        String description = "Good bear!";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data" .getBytes());
        given(bottleCapService.addCapItem(capName, description, file)).willThrow(new GoogleDriveException());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/caps")
                        .file(file)
                        .param("name", capName)
                        .param("desc", description))
                .andExpect(status().is(400));
    }

    @Test
    public void testValidateCap() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data" .getBytes());
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
    public void testWhatCapYouAre() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data" .getBytes());
        given(bottleCapService.validateWhatCapYouAre(fileName, file)).willReturn(new BottleCapDto());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/whatCapAreYou")
                        .file(file)
                        .param("name", fileName))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['id']", is(0)));
    }

    @Test
    public void getBottleCapsSuccess() throws Exception {

        BottleCapDto cap = new BottleCapDto();
        cap.setCapName("cap1");
        cap.setFileLocation("location1");
        cap.setCreationDate(LocalTime.now().toString());
        BottleCapDto cap1 = new BottleCapDto();
        cap1.setCapName("cap2");
        cap1.setFileLocation("location2");
        cap1.setCreationDate(LocalTime.now().toString());
        List<BottleCapDto> allCaps = Arrays.asList(cap, cap1);
        given(bottleCapService.getAllBottleCapsDto()).willReturn(allCaps);

        mvc.perform(get("/caps")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fileLocation", is("location1")))
                .andExpect(jsonPath("$[0].capName", is("cap1")))
                .andExpect(jsonPath("$[0].creationDate").isNotEmpty())
                .andExpect(jsonPath("$[0].description").isEmpty())
                .andExpect(jsonPath("$[1].fileLocation", is("location2")))
                .andExpect(jsonPath("$[1].capName", is("cap2")))
                .andExpect(jsonPath("$[1].creationDate").isNotEmpty())
                .andExpect(jsonPath("$[1].description").isEmpty());

    }

    @Test
    public void getLinksSuccess() throws Exception {
        BottleCapPictureDto cap = new BottleCapPictureDto();
        cap.setId(1);
        cap.setUrl("link");
        BottleCapPictureDto cap1 = new BottleCapPictureDto();
        cap1.setId(2);
        cap1.setUrl("link1");
        List<BottleCapPictureDto> allCaps = Arrays.asList(cap, cap1);

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
    public void getCatalogSuccess() throws Exception {
        BottleCapCatalogDto cap = new BottleCapCatalogDto();
        cap.setName("cap1");
        cap.setDescription("Good beer!");
        cap.setUrl("link");

        BottleCapCatalogDto cap1 = new BottleCapCatalogDto();
        cap1.setName("cap2");
        cap1.setDescription("The best beer!!");
        cap1.setUrl("link1");

        List<BottleCapCatalogDto> allCaps = Arrays.asList(cap, cap1);

        given(bottleCapService.getCatalogCaps()).willReturn(allCaps);

        mvc.perform(get("/catalog")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(0)))
                .andExpect(jsonPath("$[0].url", is("link")))
                .andExpect(jsonPath("$[0].name", is("cap1")))
                .andExpect(jsonPath("$[0].description", is("Good beer!")))
                .andExpect(jsonPath("$[1].id", is(0)))
                .andExpect(jsonPath("$[1].url", is("link1")))
                .andExpect(jsonPath("$[1].name", is("cap2")))
                .andExpect(jsonPath("$[1].description", is("The best beer!!")));
    }

    @Test
    public void getBottleCapSuccess() throws Exception {
        BottleCapDto bottleCapDto = new BottleCapDto();
        bottleCapDto.setCapName("cap1");
        bottleCapDto.setFileLocation("");
        bottleCapDto.setCreationDate("2021-01-09T19:48:51.438");
        given(bottleCapService.getCapItemDto(anyLong())).willReturn(bottleCapDto);
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['fileLocation']").isEmpty())
                .andExpect(jsonPath("$['capName']", is("cap1")))
                .andExpect(jsonPath("$['creationDate']").isNotEmpty())
                .andExpect(jsonPath("$['description']").isEmpty());
    }

    @Test
    public void getBottleCapWrongIDException() throws Exception {
        long id = 1;
        given(bottleCapService.getCapItemDto(id)).willThrow(new CapNotFoundException(id));
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateBottleCapWithAdminRoleSuccess() throws Exception {
        CapItem cap = new CapItem();
        cap.setName("cap1");
        given(bottleCapService.getCapItem(anyLong())).willReturn(cap);
        this.mvc.perform(put("/caps/1")
                        .param("newName", "Beer")
                        .param("newDesc", "Good beer"))
                .andExpect(status().is(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateBottleCapWrongIDException() throws Exception {
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
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCapWithAdminRoleSuccess() throws Exception {
        CapItem cap = new CapItem();
        cap.setName("cap1");
        given(bottleCapService.getCapItem(anyLong())).willReturn(cap);
        given(googleDriveService.deleteFile(anyString())).willReturn("");
        this.mvc.perform(delete("/caps/1"))
                .andExpect(status().is(200));

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCapNotFoundWithAdminRoleException() throws Exception {
        long capId = 12L;
        doThrow(new CapNotFoundException(capId)).when(bottleCapService).removeCapItem(capId);
        this.mvc.perform(delete("/caps/" + capId))
                .andExpect(status().is(404));
    }
}
