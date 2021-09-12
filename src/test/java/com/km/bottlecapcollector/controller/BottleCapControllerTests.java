package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.dto.CustomMapper;
import com.km.bottlecapcollector.exception.CapNotFoundException;
import com.km.bottlecapcollector.google.GoogleDriveService;
import com.km.bottlecapcollector.model.BottleCap;
import com.km.bottlecapcollector.service.BottleCapService;
import com.km.bottlecapcollector.service.ComparisonRangeService;
import com.km.bottlecapcollector.service.FileStorageService;
import com.km.bottlecapcollector.util.BottleCapMat;
import com.km.bottlecapcollector.util.SimilarityModel;
import liquibase.pro.packaged.A;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modelmapper.ModelMapper;
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
    private BottleCapService service;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private ComparisonRangeService comparisonRangeService;

    @MockBean
    private GoogleDriveService googleDriveService;

    @MockBean
    private DataSource dataSource;

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
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(fileStorageService.calculateIntersectionMethod(any())).willReturn(123d);
        given(googleDriveService.uploadFile(any())).willReturn("abcdfgh123");
        given(fileStorageService.convertMathObjectToBottleCapMat(any())).
                willReturn(new BottleCapMat("43drgdsgre".getBytes(), 50, 60));
        given(service.addBottleCap(any())).willReturn(new BottleCap());


        this.mvc.perform(MockMvcRequestBuilders.multipart("/caps")
                .file(file)
                .param("name", "Beer")
                .param("desc", "Good bear!"))
                .andExpect(status().is(201))
                .andExpect(jsonPath("$", is(0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void addBottleCapWithAdminRoleThrowsException() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(fileStorageService.calculateIntersectionMethod(any())).willReturn(123d);
        given(googleDriveService.uploadFile(any())).willReturn("abcdfgh123");
        given(fileStorageService.convertMathObjectToBottleCapMat(any())).
                willThrow(IOException.class);

        this.mvc.perform(MockMvcRequestBuilders.multipart("/caps")
                .file(file)
                .param("name", "Beer")
                .param("desc", "Good bear!"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$", is(-1)));
    }

    @Test
    public void testValidateCap() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(fileStorageService.calculateIntersectionMethod(any())).willReturn(123d);
        given(googleDriveService.uploadFile(any())).willReturn("abcdfgh123");
        given(fileStorageService.convertMathObjectToBottleCapMat(any())).
                willReturn(new BottleCapMat("43drgdsgre".getBytes(), 50, 60));
        given(comparisonRangeService.calculateSimilarityModelForCap(any(), anyInt()))
                .willReturn(new SimilarityModel());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/validateCap")
                .file(file)
                .param("name", "Beer"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['similarCapsIDs']", hasSize(0)))
                .andExpect(jsonPath("$['similarCapsURLs']", hasSize(0)))
                .andExpect(jsonPath("$['similarityDistribution']", hasSize(10)))
                .andExpect(jsonPath("$['duplicate']", is(false)));
    }

    @Test
    public void testWhatCapYouAre() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(fileStorageService.calculateIntersectionMethod(any())).willReturn(123d);
        given(googleDriveService.uploadFile(any())).willReturn("abcdfgh123");
        given(fileStorageService.convertMathObjectToBottleCapMat(any())).
                willReturn(new BottleCapMat("43drgdsgre".getBytes(), 50, 60));
        given(comparisonRangeService.calculateSimilarityModelForCap(any(), anyInt()))
                .willReturn(new SimilarityModel());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/whatCapAreYou")
                .file(file)
                .param("name", "Beer"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$['id']", is(0)));
    }

    @Test
    public void getBottleCapsSuccess() throws Exception {

        BottleCap cap = new BottleCap("cap1");
        BottleCap cap1 = new BottleCap("cap2");
        List<BottleCap> allCaps = Arrays.asList(cap, cap1);
        given(service.getAllBottleCaps()).willReturn(allCaps);

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

        given(service.getAllBottleCaps()).willReturn(allCaps);

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

        given(service.getAllBottleCaps()).willReturn(allCaps);

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
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
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
        given(service.getBottleCap(id)).willThrow(new CapNotFoundException(id));
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateBottleCapWithAdminRoleSuccess() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
        this.mvc.perform(put("/caps/1")
                .param("newName", "Beer")
                .param("newDesc", "Good beer"))
                .andExpect(status().is(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateBottleCapWrongIDException() throws Exception {
        long id = 1;
        given(service.getBottleCap(id)).willThrow(new CapNotFoundException(id));
        this.mvc.perform(put("/caps/1")
                .param("newName", "Beer")
                .param("newDesc", "Good beer"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCapWithAdminRoleSuccess() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
        given(googleDriveService.deleteFile(anyString())).willReturn("");
        this.mvc.perform(delete("/caps/1"))
                .andExpect(status().is(200));

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCapNotFoundWithAdminRoleException() throws Exception {
        given(service.getBottleCap(1)).willThrow(new IllegalArgumentException());
        this.mvc.perform(delete("/caps/1"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCapWrongDriveIDWithAdminRoleNotFound() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
        given(googleDriveService.deleteFile(any())).willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        this.mvc.perform(delete("/caps/1"))
                .andExpect(status().is(400));
    }


    @Test
    public void getComparisonRangeValuesSuccess() throws Exception {
        this.mvc.perform(get("/comparisonRangeValues"))
                .andExpect(status().is(200));
    }
}
