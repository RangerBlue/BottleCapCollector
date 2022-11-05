package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.service.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = MaintenanceController.class)
public class MaintenanceControllerTests {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private MaintenanceService maintenanceService;

    @MockBean
    private DataSource dataSource;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void addFileToDriveWithAdminRoleSuccess() throws Exception {
        String fileName = "captest1.jpg";
        MockMultipartFile file = new MockMultipartFile("file", fileName,
                "text/plain", "test data".getBytes());
        given(maintenanceService.uploadFileToGoogleDrive(any())).willReturn("abcdfgh123");

        this.mvc.perform(MockMvcRequestBuilders.multipart("/admin/upload-file-to-google-drive")
                        .file(file))
                .andExpect(status().is(200));
    }
}
