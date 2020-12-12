package com.km.BottleCapCollector.controller;

import com.km.BottleCapCollector.google.GoogleDriveService;
import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.service.BottleCapService;
import com.km.BottleCapCollector.service.ComparisonRangeService;
import com.km.BottleCapCollector.service.FileStorageService;
import com.km.BottleCapCollector.util.BottleCapMat;
import com.km.BottleCapCollector.util.SimilarityModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpClientErrorException;

import javax.sql.DataSource;
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

        this.mvc.perform(MockMvcRequestBuilders.multipart("/caps")
                .file(file)
                .param("name", "Beer"))
                .andExpect(status().is(201));
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
        given(comparisonRangeService.calculateSimilarityModelForCap(any())).willReturn(new SimilarityModel());

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
    public void getBottleCapsSuccess() throws Exception {

        BottleCap cap = new BottleCap("cap1");
        List<BottleCap> allCaps = Arrays.asList(cap);

        given(service.getAllBottleCaps()).willReturn(allCaps);

        mvc.perform(get("/caps")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getLinksSuccess() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        cap.setFileLocation("link");
        BottleCap cap1 = new BottleCap("cap2");
        cap.setFileLocation("link1");
        List<BottleCap> allCaps = Arrays.asList(cap, cap1);

        given(service.getAllBottleCaps()).willReturn(allCaps);

        mvc.perform(get("/links")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getBottleCapSuccess() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(200));
    }

    @Test
    public void getBottleCapWrongIDException() throws Exception {
        given(service.getBottleCap(1)).willThrow(new IllegalArgumentException());
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateBottleCapWithAdminRoleSuccess() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
        this.mvc.perform(put("/caps/1")
                .param("newName", "Beer"))
                .andExpect(status().is(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateBottleCapWrongIDException() throws Exception {
        given(service.getBottleCap(1)).willThrow(new IllegalArgumentException());
        this.mvc.perform(put("/caps/1")
                .param("newName", "Beer"))
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
