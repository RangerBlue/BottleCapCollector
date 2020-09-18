package com.km.BottleCapCollector.controller;

import com.km.BottleCapCollector.google.GoogleDriveService;
import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.service.BottleCapService;
import com.km.BottleCapCollector.service.ComparisonRangeService;
import com.km.BottleCapCollector.service.FileStorageService;
import com.km.BottleCapCollector.util.BottleCapMat;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    public void addFileToDriveTest() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file",fileName,
                "text/plain", "test data".getBytes());
        given(googleDriveService.uploadFile(any())).willReturn("abcdfgh123");

        this.mvc.perform(MockMvcRequestBuilders.multipart("/uploadFileToDrive")
                .file(file))
                .andExpect(status().is(200));
    }

    @Test
    public void addBottleCapTest() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file",fileName,
                "text/plain", "test data".getBytes());
        given(googleDriveService.uploadFile(any())).willReturn("abcdfgh123");
        given(fileStorageService.calculateAndReturnMathObjectAsBottleCapMat(any())).willReturn(new BottleCapMat("test".getBytes(), 50, 60));

        this.mvc.perform(MockMvcRequestBuilders.multipart("/addCap")
                .file(file)
                .param("name", "Beer"))
                .andExpect(status().is(201));
    }

    @Test
    @WithMockUser("user")
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
        this.mvc.perform(get("/cap/1"))
                .andExpect(status().is(200));
    }


    @Test
    public void getComparisonRangeValues() throws Exception {
        this.mvc.perform(get("/comparisonRangeValues"))
                .andExpect(status().is(200));
    }
}
