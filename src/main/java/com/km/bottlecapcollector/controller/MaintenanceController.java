package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.dto.ComparisonRangeDto;
import com.km.bottlecapcollector.service.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@CrossOrigin
@AllArgsConstructor
@Slf4j
@RequestMapping("admin")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    @Async()
    @PutMapping("/update-google-drive-data")
    public void updateCapLocations() {
        log.warn("Requested picture update on demand");
        maintenanceService.updateGoogleDrivePicturesUrls();
    }

    @PostMapping("/upload-file-to-google-drive")
    public String uploadFileToDrive(@RequestParam("file") MultipartFile multipartFile) {
        log.warn("Requested picture update on demand");
        return maintenanceService.uploadFileToGoogleDrive(multipartFile);
    }

    @PostMapping("/prepare-open-cv-data")
    public ResponseEntity<List<ComparisonRangeDto>> prepareOpenCvData() {
        log.warn("Requested open cv data preparation on demand");
        return ResponseEntity.ok().body(maintenanceService.calculateComparisonRangeData());
    }

    @Async()
    @PostMapping("/run-initial-caps-import")
    public ResponseEntity<String> uploadCapsFromLocalStorage() {
        log.warn("Requested run initial caps import on demand");
        maintenanceService.uploadCapsFromLocalStorage();
        return ResponseEntity.ok("Requested run initial caps import on demand");
    }

    @Async()
    @PostMapping("/run-initial-data-import")
    public ResponseEntity<String> prepareData() {
        log.warn("Requested run initial data import on demand");
        uploadCapsFromLocalStorage();
        prepareOpenCvData();
        return ResponseEntity.ok("Requested run initial data import on demand");
    }
}
