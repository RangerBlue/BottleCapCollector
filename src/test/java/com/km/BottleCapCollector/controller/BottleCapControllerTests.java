package com.km.BottleCapCollector.controller;

import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.service.BottleCapService;
import com.km.BottleCapCollector.service.FileStorageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(BottleCapController.class)
public class BottleCapControllerTests {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BottleCapService service;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    public void addBottleCap() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file",fileName,
                "text/plain", "test data".getBytes());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/addCap")
                .file(file)
                .param("capName", "Beer"))
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
        this.mvc.perform(get("/cap/1"))
                .andExpect(status().is(200));
    }

    @Test
    public void uploadMultipleFiles() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file",fileName,
                "text/plain", "test data".getBytes());

        this.mvc.perform(MockMvcRequestBuilders.multipart("/uploadMultipleFiles")
                .file(file)
                .file(file)
                .param("capName", "Beer"))
                .andExpect(status().is(200));
    }
}
