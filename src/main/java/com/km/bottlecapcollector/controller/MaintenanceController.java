package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.google.GoogleDriveService;
import com.km.bottlecapcollector.model.AbstractImage;
import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.opencv.HistogramResult;
import com.km.bottlecapcollector.opencv.ImageHistogramUtil;
import com.km.bottlecapcollector.service.AbstractImageService;
import com.km.bottlecapcollector.service.BottleCapService;
import com.km.bottlecapcollector.service.ComparisonRangeService;
import com.km.bottlecapcollector.service.LocalFileStorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@CrossOrigin
@AllArgsConstructor
@Slf4j
@RequestMapping("admin")
public class MaintenanceController {

    private final BottleCapService bottleCapService;

    private final LocalFileStorageService localFileStorageService;

    private final GoogleDriveService googleDriveService;

    private final ComparisonRangeService comparisonRangeService;

    private final AbstractImageService abstractImageService;

    @Async()
    @PutMapping("/updateThumbnailsURL")
    public void updateCapLocations() {
        log.info("Entering updateCapLocations method");
        List<? extends AbstractImage> images = abstractImageService.getAllGoogleDriveImages();
        AtomicInteger counter = new AtomicInteger();
        images.forEach(image -> {
            image.setUrl(googleDriveService.getFileUrl(image.getProvider().getImageProviderId()));
            image.getProvider().setUpdateDateTime(LocalDateTime.now());
            abstractImageService.updateGoogleDriveImage(image);
            counter.getAndIncrement();
        });
        log.info("Updated " + counter + " locations");
    }

    @PostMapping("/uploadFileToDrive")
    public String uploadFileToDrive(@RequestParam("file") MultipartFile multipartFile) {
        return googleDriveService.uploadFile(multipartFile);
    }

    @GetMapping(value = "/capDrive/{id}")
    public String getFile(@PathVariable String id) {
        return googleDriveService.getFileUrl(id);

    }

    /**
     * Counts all pictures which are in file storage folder
     *
     * @return amount of pictures
     */
    @GetMapping("/admin/countAll")
    public long countAllCapsImages() {
        return localFileStorageService.countAllFiles();
    }


    /**
     * Calculates OpenCv coefficients of unique permutation of two pictures, each picture will be calculated against
     * rest of them
     *
     * @return Http status
     */
    @PostMapping("/calculateEachWithEachCap")
    public ResponseEntity<String> calculateEachWithEachCap() {
        List<CapItem> caps = bottleCapService.getAllCapItems();
        List<HistogramResult> histogramResults = ImageHistogramUtil.calculateEachWithEachCap(caps);
        comparisonRangeService.calculateMinMaxValuesOfAllComparisonMethods(histogramResults);
        return ResponseEntity.ok("Open CV coefficients have been calculated");
    }

    /**
     * Adds BottleCap object and creates Mat objects in object storage folder from each file picture which is right now
     * in file storage folder
     *
     * @return Http status
     */
    @PostMapping("/addAndCalculateAllPictures")
    public ResponseEntity<String> addAndCalculateAllPictures() {
        localFileStorageService.getAllPictures().forEach(file -> {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                MultipartFile multipartFile = new MockMultipartFile(file.getName(), file.getName(),
                        ContentType.IMAGE_JPEG.toString(), fileInputStream);
                bottleCapService.addCapItem(file.getName(), "This cap does not have description yet",
                        multipartFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        return ResponseEntity.ok("All pictures have been added as bottle caps");
    }

    @PostMapping("/prepareData")
    public ResponseEntity<String> prepareData() {
        addAndCalculateAllPictures();
        calculateEachWithEachCap();
        return ResponseEntity.ok("Data preparation has been finished");
    }
}
