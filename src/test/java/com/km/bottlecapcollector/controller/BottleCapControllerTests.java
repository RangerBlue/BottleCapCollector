package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.dto.BottleCapDto;
import com.km.bottlecapcollector.dto.BottleCapValidationResponseDto;
import com.km.bottlecapcollector.dto.CustomMapper;
import com.km.bottlecapcollector.exception.CapNotFoundException;
import com.km.bottlecapcollector.exception.GoogleDriveException;
import com.km.bottlecapcollector.google.GoogleDriveService;
import com.km.bottlecapcollector.model.BottleCap;
import com.km.bottlecapcollector.service.ItemService;
import com.km.bottlecapcollector.service.ComparisonRangeService;
import com.km.bottlecapcollector.service.FileStorageService;
import com.km.bottlecapcollector.util.CustomMat;
import com.km.bottlecapcollector.util.ImageHistogramUtil;
import com.km.bottlecapcollector.util.ItemFactory;
import com.km.bottlecapcollector.util.SimilarityModel;
import liquibase.pro.packaged.B;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpClientErrorException;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = BottleCapController.class)
@Import(CustomMapper.class)
public class BottleCapControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ItemService itemService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private ComparisonRangeService comparisonRangeService;

    @MockBean
    private GoogleDriveService googleDriveService;

    @MockBean
    private DataSource dataSource;

    private ImageHistogramUtil imageHistogramUtil = new ImageHistogramUtil();

    @Test
    @WithMockUser(roles = "ADMIN")
    public void addFileToDriveWithAdminRoleSuccess() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(googleDriveService.uploadFile(any())).willReturn("abcdfgh123");

        this.mvc.perform(MockMvcRequestBuilders.multipart("/admin/uploadFileToDrive")
                .file(file))
                .andExpect(status().is(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void addBottleCapWithAdminRoleSuccess() throws Exception {
        String fileName = "captest1.jpg";
        String capName = "Beer";
        String description = "Good bear!";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(itemService.addCapItem(capName, description, file)).willReturn(12L);


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
                "text/plain", "test data".getBytes());
        given(itemService.addCapItem(capName, description, file)).willThrow(new GoogleDriveException());

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
                "text/plain", "test data".getBytes());
        given(itemService.validateCapItem(fileName, file)).willReturn(new BottleCapValidationResponseDto(false,
                Arrays.asList(1L, 2L), Arrays.asList("www.google.pl", "www.google.pl"),
                new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8 ,9}));

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
                "text/plain", "test data".getBytes());
        given(itemService.validateWhatCapYouAre(fileName, file)).willReturn(new BottleCapDto());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/whatCapAreYou")
                .file(file)
                .param("name", fileName))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['id']", is(0)));
    }

    @Test
    public void getBottleCapsSuccess() throws Exception {

        BottleCap cap = new BottleCap("cap1");
        BottleCap cap1 = new BottleCap("cap2");
        List<BottleCap> allCaps = Arrays.asList(cap, cap1);
        given(itemService.getAllBottleCaps()).willReturn(allCaps);

        mvc.perform(get("/caps")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fileLocation").isEmpty())
                .andExpect(jsonPath("$[0].googleDriveID").isEmpty())
                .andExpect(jsonPath("$[0].capName", is("cap1")))
                .andExpect(jsonPath("$[0].creationDate").isNotEmpty())
                .andExpect(jsonPath("$[0].cols", is(0)))
                .andExpect(jsonPath("$[0].rows", is(0)))
                .andExpect(jsonPath("$[0].lastPreviewLinkUpdate").isEmpty())
                .andExpect(jsonPath("$[0].intersectionValue", is(0d)))
                .andExpect(jsonPath("$[0].description").isEmpty())
                .andExpect(jsonPath("$[1].fileLocation").isEmpty())
                .andExpect(jsonPath("$[1].googleDriveID").isEmpty())
                .andExpect(jsonPath("$[1].capName", is("cap2")))
                .andExpect(jsonPath("$[1].creationDate").isNotEmpty())
                .andExpect(jsonPath("$[1].cols", is(0)))
                .andExpect(jsonPath("$[1].rows", is(0)))
                .andExpect(jsonPath("$[1].lastPreviewLinkUpdate").isEmpty())
                .andExpect(jsonPath("$[1].intersectionValue", is(0d)))
                .andExpect(jsonPath("$[1].description").isEmpty());

    }

    @Test
    public void getLinksSuccess() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        cap.setFileLocation("link");
        BottleCap cap1 = new BottleCap("cap2");
        cap1.setFileLocation("link1");
        List<BottleCap> allCaps = Arrays.asList(cap, cap1);

        given(itemService.getAllBottleCaps()).willReturn(allCaps);

        mvc.perform(get("/links")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(0)))
                .andExpect(jsonPath("$[0].url", is("link")))
                .andExpect(jsonPath("$[1].id", is(0)))
                .andExpect(jsonPath("$[1].url", is("link1")));
    }

    @Test
    public void getCatalogSuccess() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        cap.setFileLocation("link");
        cap.setDescription("Good bear!");
        BottleCap cap1 = new BottleCap("cap2");
        cap1.setFileLocation("link1");
        cap1.setDescription("The best bear!!");
        List<BottleCap> allCaps = Arrays.asList(cap, cap1);

        given(itemService.getAllBottleCaps()).willReturn(allCaps);

        mvc.perform(get("/catalog")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(0)))
                .andExpect(jsonPath("$[0].url", is("link")))
                .andExpect(jsonPath("$[0].name", is("cap1")))
                .andExpect(jsonPath("$[0].description", is("Good bear!")))
                .andExpect(jsonPath("$[1].id", is(0)))
                .andExpect(jsonPath("$[1].url", is("link1")))
                .andExpect(jsonPath("$[1].name", is("cap2")))
                .andExpect(jsonPath("$[1].description", is("The best bear!!")));
    }

    @Test
    public void getBottleCapSuccess() throws Exception {
        BottleCapDto bottleCapDto = new BottleCapDto();
        bottleCapDto.setCapName("cap1");
        bottleCapDto.setFileLocation("");
        bottleCapDto.setGoogleDriveID("");
        bottleCapDto.setCreationDate("2021-01-09T19:48:51.438");
        given(itemService.getCapItemDto(anyLong())).willReturn(bottleCapDto);
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['fileLocation']").isEmpty())
                .andExpect(jsonPath("$['googleDriveID']").isEmpty())
                .andExpect(jsonPath("$['capName']", is("cap1")))
                .andExpect(jsonPath("$['creationDate']").isNotEmpty())
                .andExpect(jsonPath("$['cols']", is(0)))
                .andExpect(jsonPath("$['rows']", is(0)))
                .andExpect(jsonPath("$['lastPreviewLinkUpdate']").isEmpty())
                .andExpect(jsonPath("$['intersectionValue']", is(0d)))
                .andExpect(jsonPath("$['description']").isEmpty());
    }

    @Test
    public void getBottleCapWrongIDException() throws Exception {
        long id = 1;
        given(itemService.getCapItemDto(id)).willThrow(new CapNotFoundException(id));
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateBottleCapWithAdminRoleSuccess() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(itemService.getBottleCap(anyLong())).willReturn(cap);
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
        given(itemService.updateCapItemDto(id, newName, newDesc)).willThrow(new CapNotFoundException(id));
        this.mvc.perform(put("/caps/1")
                .param("newName", newName)
                .param("newDesc", newDesc))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCapWithAdminRoleSuccess() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(itemService.getBottleCap(anyLong())).willReturn(cap);
        given(googleDriveService.deleteFile(anyString())).willReturn("");
        this.mvc.perform(delete("/caps/1"))
                .andExpect(status().is(200));

    }
    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCapNotFoundWithAdminRoleException() throws Exception {
        long capId = 12L;
        given(itemService.removeCapItem(12L)).willThrow(new CapNotFoundException(capId));
        this.mvc.perform(delete("/caps/"+capId))
                .andExpect(status().is(404));
    }


    @Test
    public void getComparisonRangeValuesSuccess() throws Exception {
        this.mvc.perform(get("/comparisonRangeValues"))
                .andExpect(status().is(200));
    }
}
