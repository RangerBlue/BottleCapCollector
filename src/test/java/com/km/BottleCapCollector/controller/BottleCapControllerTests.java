package com.km.BottleCapCollector.controller;

import com.km.BottleCapCollector.google.GoogleDriveService;
import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.service.BottleCapService;
import com.km.BottleCapCollector.service.ComparisonRangeService;
import com.km.BottleCapCollector.service.FileStorageService;
import com.km.BottleCapCollector.util.BottleCapMat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    public void addFileToDriveTest() throws Exception {
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
    public void addBottleCapTest() throws Exception {
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
    public void getBottleCaps() throws Exception {

        BottleCap cap = new BottleCap("cap1");
        List<BottleCap> allCaps = Arrays.asList(cap);

        given(service.getAllBottleCaps()).willReturn(allCaps);

        mvc.perform(get("/caps")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getBottleCap() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(200));
    }

    @Test
    public void getBottleCapWrongID() throws Exception {
        given(service.getBottleCap(1)).willThrow(new IllegalArgumentException());
        this.mvc.perform(get("/caps/1"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateBottleCap() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
        this.mvc.perform(put("/caps/1")
                .param("newName", "Beer"))
                .andExpect(status().is(200));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateBottleCapWrongID() throws Exception {
        given(service.getBottleCap(1)).willThrow(new IllegalArgumentException());
        this.mvc.perform(put("/caps/1")
                .param("newName", "Beer"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCap() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
        given(googleDriveService.deleteFile(anyString())).willReturn("");
        this.mvc.perform(delete("/caps/1"))
                .andExpect(status().is(200));

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCapNotFound() throws Exception {
        given(service.getBottleCap(1)).willThrow(new IllegalArgumentException());
        this.mvc.perform(delete("/caps/1"))
                .andExpect(status().is(404));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteBottleCapWrongDriveID() throws Exception {
        BottleCap cap = new BottleCap("cap1");
        given(service.getBottleCap(anyLong())).willReturn(cap);
        given(googleDriveService.deleteFile(any())).willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        this.mvc.perform(delete("/caps/1"))
                .andExpect(status().is(400));
    }


    @Test
    public void getComparisonRangeValues() throws Exception {
        this.mvc.perform(get("/comparisonRangeValues"))
                .andExpect(status().is(200));
    }
}
