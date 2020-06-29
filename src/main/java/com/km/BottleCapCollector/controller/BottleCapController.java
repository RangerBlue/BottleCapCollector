package com.km.BottleCapCollector.controller;

import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.model.ComparisonRange;
import com.km.BottleCapCollector.model.HistogramResult;
import com.km.BottleCapCollector.payload.UploadFileResponse;
import com.km.BottleCapCollector.service.BottleCapService;
import com.km.BottleCapCollector.service.ComparisonRangeService;
import com.km.BottleCapCollector.service.FileStorageService;
import com.km.BottleCapCollector.util.SimilarityModel;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class BottleCapController {
    private static final Logger logger = LoggerFactory.getLogger(BottleCapController.class);

    @Autowired
    private BottleCapService bottleCapService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ComparisonRangeService comparisonRangeService;

    @PostMapping("/addCap")
    public ResponseEntity<BottleCap> addBottleCap(String capName, @RequestParam("file") MultipartFile file) {
        logger.debug("Entering addBottleCap method");
        BottleCap cap = new BottleCap(capName);
        UploadFileResponse response = uploadFile(file);
        String fileName = response.getFileName();
        fileStorageService.calculateAndStoreMathObject(fileName);
        cap.setFileLocation(response.getFileDownloadUri());
        bottleCapService.addBottleCap(cap);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/validateCap")
    public SimilarityModel validateBottleCap(String capName, @RequestParam("file") MultipartFile file) {
        BottleCap cap = new BottleCap(capName);
        UploadFileResponse response = uploadTemporaryFile(file);
        String fileName = response.getFileName();
        Mat mat = fileStorageService.calculateAndReturnMathObject(fileName);
        cap.setFileLocation(response.getFileDownloadUri());
        List<BottleCap> caps = new ArrayList<>(bottleCapService.getAllBottleCaps());
        BottleCap savedCap = bottleCapService.addBottleCap(cap);
        List<HistogramResult> histogramResults =  fileStorageService.calculateOneAgainstAllCaps(savedCap, mat, caps);
        bottleCapService.deleteBottleCapWithId(savedCap.getId());
        return comparisonRangeService.calculateSimilarityModelForCap(histogramResults);
    }


    @GetMapping("/caps")
    public List<BottleCap> getBottleCaps() {
        logger.debug("Entering getBottleCaps method");
        return bottleCapService.getAllBottleCaps();
    }

    @GetMapping("/cap/{id}")
    public BottleCap getBottleCap(@PathVariable Long id) {
        logger.debug("getBottleCap");
        return bottleCapService.getBottleCap(id);
    }

    @PostMapping("/uploadFile")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        logger.debug("Entering uploadFile method");
        String fileName = fileStorageService.storeFileInPicturesFolder(file);


        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileName)
                .toUriString();

        logger.debug("Exiting to uploadFile method");
        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
    }

    @PostMapping("/uploadTemporaryFile")
    public UploadFileResponse uploadTemporaryFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFileInTemporaryPicturesFolder(file);


        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileName)
                .toUriString();

        logger.debug("Exiting to uploadFile method");
        return new UploadFileResponse(fileName, fileDownloadUri,
                file.getContentType(), file.getSize());
    }

    @PostMapping("/uploadMultipleFiles")
    public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        logger.debug("Entering uploadMultipleFiles method");
        return Arrays.asList(files)
                .stream()
                .map(file -> uploadFile(file))
                .collect(Collectors.toList());
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        logger.debug("Entering downloadFile method");
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            logger.debug("Could not determine file type.");
        }

        logger.debug("Exiting downloadFile method");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/downloadCapFile/{id}")
    public ResponseEntity<Resource> downloadFileByCapId(HttpServletRequest request, @PathVariable Long id) {
        BottleCap cap = bottleCapService.getBottleCap(id);
        logger.debug("downloadCapFile method");
        return downloadFile(cap.getFileLocation().substring(cap.getFileLocation().lastIndexOf('/') + 1), request);
    }

    /**
     * Counts all pictures which are in file storage folder
     *
     * @return amount of pictures
     */
    @GetMapping("/countAll")
    public long countAllCapsImages() {
        return fileStorageService.countAllFiles();
    }

    /**
     * Creates Mat object in object storage folder from each file picture which is right now in file storage folder
     *
     * @return Http status
     */
    @PostMapping("/admin/processAll")
    public ResponseEntity processAll() {
        return new ResponseEntity<>("Elements processed " + fileStorageService.processAllFiles(), HttpStatus.OK);
    }

    /**
     * Calculates OpenCv coefficients of unique permutation of two pictures, each picture will be calculated against
     * rest of them
     *
     * @return Http status
     */
    @PostMapping("/admin/calculateEachWithEachCap")
    public ResponseEntity calculateEachWithEachCap() {
        List<BottleCap> caps = bottleCapService.getAllBottleCaps().stream().collect(Collectors.toList());
        fileStorageService.calculateEachWithEachCap(caps);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Adds BottleCap object and creates Mat objects in object storage folder from each file picture which is right now
     * in file storage folder
     *
     * @return Http status
     */
    @PostMapping("/admin/addAndCalculateAllPictures")
    public ResponseEntity addAndCalculateAllPictures() {
        fileStorageService.getAllPictures().forEach(file -> {
            bottleCapService.addBottleCapForInitialUpload(file);
            fileStorageService.calculateAndStoreMathObject(file.getName());
        });
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/admin/updateLocationOfAllPictures")
    public ResponseEntity updateLocationOfAllPictures() {
        bottleCapService.getAllBottleCaps().stream().parallel()
                .forEach(bottleCap -> {
                    bottleCap.setFileLocation(fileStorageService.fileStorageLocation + System.getProperty("file.separator") + bottleCap.getCapName());
                    bottleCapService.addBottleCap(bottleCap);
                });
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/admin/prepareData")
    public ResponseEntity prepareData() {
        try {
            Files.walk(fileStorageService.objectStorageLocation).map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            return new ResponseEntity<>(null, null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        addAndCalculateAllPictures();
        calculateEachWithEachCap();
        updateLocationOfAllPictures();
        calculateMethodMinMaxValues();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/admin/calculateMethodMaxMinValues")
    public ResponseEntity calculateMethodMinMaxValues() {
        try {
            comparisonRangeService.calculateMinMaxValuesOfAllComparisonMethods(fileStorageService.getAllHistogramResults());
        } catch (Exception e) {
            return new ResponseEntity<>(null, null, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/comparisonRangeValues")
    public List<ComparisonRange> getRangeValues() {
        return comparisonRangeService.getAll();
    }

}
