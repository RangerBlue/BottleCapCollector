package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.dto.ComparisonRangeDto;
import com.km.bottlecapcollector.google.GoogleDriveService;
import com.km.bottlecapcollector.mapper.ComparisonRangeMapper;
import com.km.bottlecapcollector.model.AbstractImage;
import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.model.ComparisonRange;
import com.km.bottlecapcollector.opencv.HistogramResult;
import com.km.bottlecapcollector.opencv.ImageHistogramUtil;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class MaintenanceService {
    private final AbstractImageService abstractImageService;
    private final GoogleDriveService googleDriveService;
    private final BottleCapService bottleCapService;
    private final ComparisonRangeService comparisonRangeService;
    private final LocalFileStorageService localFileStorageService;

    public void updateGoogleDrivePicturesUrls(){
        List<? extends AbstractImage> images = abstractImageService.getAllGoogleDriveImages();
        int listSize = images.size();
        log.info("Updating {} pictures", listSize);
        images.forEach(image -> {
            log.info("Updating image {}", image.getId());
            image.setUrl(googleDriveService.getFileUrl(image.getProvider().getImageProviderId()));
            image.getProvider().setUpdateDateTime(LocalDateTime.now());
            abstractImageService.updateGoogleDriveImage(image);
        });
        log.info("Updated {} locations", listSize);
    }
    public String uploadFileToGoogleDrive(MultipartFile multipartFile){
        return googleDriveService.uploadFile(multipartFile);
    }
    @Scheduled(cron = "${bcc.google-drive-update-cron}")
    public void runScheduledAction(){
        log.info("Started update of Google Drive images");
        updateGoogleDrivePicturesUrls();
        log.info("Finished update of Google Drive images");
    }

    public List<ComparisonRangeDto> calculateComparisonRangeData() {
        List<CapItem> caps = bottleCapService.getAllCapItems();
        if(caps.isEmpty()){
            log.error("There is no caps to be calculated, stopping process.");
            return Collections.emptyList();
        }
        List<HistogramResult> histogramResults = ImageHistogramUtil.calculateEachWithEachCap(caps);
        List<ComparisonRange> comparisonRanges =
                comparisonRangeService.calculateMinMaxValuesOfAllComparisonMethods(histogramResults);
        log.info("Open CV coefficients have been calculated");
        return comparisonRanges.stream().map(ComparisonRangeMapper.INSTANCE::comparisonRangeToComparisonRangeDto).collect(Collectors.toList());
    }

    public void uploadCapsFromLocalStorage() {
        log.info("Uploading caps from {}", localFileStorageService.getUploadFolderLocation());
        localFileStorageService.getAllPictures().forEach(file -> {
            log.info("Uploading file {}", file.getName());
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(file);
                MultipartFile multipartFile = new MockMultipartFile(file.getName(), file.getName(),
                        ContentType.IMAGE_JPEG.toString(), fileInputStream);
                bottleCapService.addCapItem(file.getName(), "This cap does not have description yet",
                        multipartFile);
            } catch (IOException e) {
                log.error("Error encountered during upload {}", e.getMessage());
            }
        });
        log.info("Finished uploading");
    }


}
