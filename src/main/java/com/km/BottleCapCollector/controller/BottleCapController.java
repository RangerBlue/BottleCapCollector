package com.km.BottleCapCollector.controller;

import com.km.BottleCapCollector.google.GoogleDriveService;
import com.km.BottleCapCollector.google.GoogleDriveUploadItem;
import com.km.BottleCapCollector.model.*;
import com.km.BottleCapCollector.payload.CapWrapper;
import com.km.BottleCapCollector.payload.ValidateBottleCapResponse;
import com.km.BottleCapCollector.service.BottleCapService;
import com.km.BottleCapCollector.service.ComparisonRangeService;
import com.km.BottleCapCollector.service.FileStorageService;
import com.km.BottleCapCollector.util.BottleCapMat;
import com.km.BottleCapCollector.util.HistogramResult;
import com.km.BottleCapCollector.payload.PictureWrapper;
import com.km.BottleCapCollector.util.SimilarityModel;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
public class BottleCapController {

    private static final Logger logger = LogManager.getLogger(BottleCapController.class);

    @Autowired
    private BottleCapService bottleCapService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ComparisonRangeService comparisonRangeService;

    @Autowired
    private GoogleDriveService googleDriveService;

    @PostMapping("/caps")
    public ResponseEntity<Long> addBottleCap(@RequestParam("name") String capName,
                                             @RequestParam("desc") String description,
                                             @RequestParam("file") MultipartFile file) {
        logger.info("Entering addBottleCap method");
        BottleCap cap;
        BottleCapMat bottleCapMatFile;
        String googleDriveID;
        String fileLocation;
        Mat mat;
        long addedID = -1;
        double intersectionValue = 0;
        try {
            googleDriveID = uploadFileToDrive(file);
            fileLocation = googleDriveService.getFileUrl(googleDriveID);
            mat = fileStorageService.calculateAndReturnMathObject(file);
            bottleCapMatFile = fileStorageService.convertMathObjectToBottleCapMat(mat);
            intersectionValue = fileStorageService.calculateIntersectionMethod(mat);
            cap = new BottleCap(capName, description, bottleCapMatFile, fileLocation, googleDriveID, intersectionValue);
            addedID = bottleCapService.addBottleCap(cap).getId();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(addedID);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(addedID);
    }


    @DeleteMapping("/caps/{id}")
    public ResponseEntity<String> deleteBottleCap(@PathVariable Long id) {
        logger.info("Entering deleteBottleCap method");
        BottleCap capToDelete;
        try {
            capToDelete = bottleCapService.getBottleCap(id);
            googleDriveService.deleteFile(capToDelete.getGoogleDriveID());
        } catch (HttpClientErrorException e) {
            logger.info("Could not remove cap " + id + " from drive");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        logger.info("Cap with ID " + id + " has been removed from drive");
        bottleCapService.deleteBottleCapWithId(capToDelete.getId());
        logger.info("Cap with ID " + id + " has been removed from database");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/caps/{id}")
    public ResponseEntity<BottleCap> getBottleCap(@PathVariable Long id) {
        logger.info("Entering getBottleCap method");
        BottleCap cap;
        try {
            cap = bottleCapService.getBottleCap(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok().body(cap);
    }

    @PutMapping("/caps/{id}")
    public ResponseEntity<BottleCap> updateCap(@PathVariable Long id, @RequestParam("newName") String newName,
                                               @RequestParam("newDesc") String newDesc) {
        logger.info("Entering updateCap method");
        BottleCap capToUpdate;
        try {
            capToUpdate = bottleCapService.getBottleCap(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        logger.info("Updating cap with name: " + capToUpdate.getCapName() + " and description " +
                capToUpdate.getDescription() + " to " + newName + " and " + newDesc);
        capToUpdate.setCapName(newName);
        capToUpdate.setDescription(newDesc);
        bottleCapService.addBottleCap(capToUpdate);
        return ResponseEntity.ok().body(capToUpdate);
    }

    @PostMapping("/validateCap")
    public ValidateBottleCapResponse validateBottleCap(@RequestParam("name") String capName, MultipartFile file)
            throws IOException {
        logger.info("Entering validateBottleCap method");
        List<BottleCap> caps = new ArrayList<>(bottleCapService.getAllBottleCaps());
        Mat mat = fileStorageService.calculateAndReturnMathObject(file);
        BottleCapMat bottleCapMat = fileStorageService.convertMathObjectToBottleCapMat(mat);
        double intersectionValue = fileStorageService.calculateIntersectionMethod(mat);
        BottleCap savedCap = new BottleCap(capName, bottleCapMat, intersectionValue);
        List<HistogramResult> histogramResults = fileStorageService.calculateOneAgainstAllCaps(savedCap, mat, caps);
        SimilarityModel similarityModel = comparisonRangeService.calculateSimilarityModelForCap(histogramResults,
                SimilarityModel.similarCapAmount);
        ArrayList<BottleCap> similarCaps = similarityModel.getSimilarCaps().stream().map(HistogramResult::getSecondCap).
                collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Long> similarCapsIDs = similarCaps.stream().map(BottleCap::getId).
                collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> similarCapsURLs = similarCaps.stream().map(BottleCap::getFileLocation).
                collect(Collectors.toCollection(ArrayList::new));
        return new ValidateBottleCapResponse(similarityModel.isDuplicate(), similarCapsIDs, similarCapsURLs,
                similarityModel.getSimilarityDistribution());
    }

    @PostMapping("/whatCapAreYou")
    public ResponseEntity<BottleCap> whatCapAreYou(@RequestParam("name") String capName, MultipartFile file)
            throws IOException {
        logger.info("Entering whatCapAreYou method");
        List<BottleCap> caps = new ArrayList<>(bottleCapService.getAllBottleCaps());
        Mat mat = fileStorageService.calculateAndReturnMathObject(file);
        BottleCapMat bottleCapMat = fileStorageService.convertMathObjectToBottleCapMat(mat);
        double intersectionValue = fileStorageService.calculateIntersectionMethod(mat);
        BottleCap savedCap = new BottleCap(capName, bottleCapMat, intersectionValue);
        List<HistogramResult> histogramResults = fileStorageService.calculateOneAgainstAllCaps(savedCap, mat, caps);
        SimilarityModel similarityModel = comparisonRangeService.calculateSimilarityModelForCap(histogramResults,
                SimilarityModel.similarCapAmountOne);
        BottleCap cap = similarityModel.getSimilarCaps().stream().map(HistogramResult::getSecondCap).findFirst().
                orElse(new BottleCap());
        return ResponseEntity.ok().body(cap);
    }


    @GetMapping("/caps")
    public List<BottleCap> getBottleCaps() {
        logger.info("Entering getBottleCaps method");
        return bottleCapService.getAllBottleCaps();
    }

    @GetMapping("/links")
    public ArrayList<PictureWrapper> getBottleCapsLinks() {
        logger.info("Entering getBottleCapsLinks method");
        return bottleCapService.getAllBottleCaps().stream().map(bottleCap ->
                new PictureWrapper(bottleCap.getId(), bottleCap.getFileLocation()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @GetMapping("/catalog")
    public ArrayList<CapWrapper> getCapCatalog() {
        logger.info("Entering getCapCatalog method");
        return bottleCapService.getAllBottleCaps().stream().map(bottleCap ->
                new CapWrapper(bottleCap.getId(), bottleCap.getFileLocation(), bottleCap.getCapName(),
                        bottleCap.getDescription()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @PostMapping("/admin/uploadFileToDrive")
    public String uploadFileToDrive(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        String contentType = multipartFile.getContentType();
        String originalFilename = multipartFile.getOriginalFilename();
        byte[] byteArray = multipartFile.getBytes();
        String fileName = multipartFile.getName();
        GoogleDriveUploadItem uploadItem = new GoogleDriveUploadItem(contentType, originalFilename, fileName, byteArray);
        return googleDriveService.uploadFile(uploadItem);
    }

    @GetMapping(value = "/admin/capDrive/{id}")
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
        return fileStorageService.countAllFiles();
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
        List<HistogramResult> histogramResults = fileStorageService.calculateEachWithEachCap(caps);
        comparisonRangeService.calculateMinMaxValuesOfAllComparisonMethods(histogramResults);
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
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                MultipartFile multipartFile = new MockMultipartFile(file.getName(), file.getName(),
                        ContentType.IMAGE_JPEG.toString(), fileInputStream);
                addBottleCap(file.getName(), "This cap does not have description yet", multipartFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/admin/prepareData")
    public ResponseEntity prepareData() {
        addAndCalculateAllPictures();
        calculateEachWithEachCap();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/comparisonRangeValues")
    public List<ComparisonRange> getRangeValues() {
        return comparisonRangeService.getAll();
    }

    @Async()
    @PutMapping("/admin/updateThumbnailsURL")
    public void updateCapLocations() {
        logger.info("Entering updateCapLocations method");
        List<BottleCap> caps = new ArrayList<>(bottleCapService.getAllBottleCaps());
        AtomicInteger counter = new AtomicInteger();
        caps.forEach(bottleCap -> {
            bottleCap.setFileLocation(googleDriveService.getFileUrl(bottleCap.getGoogleDriveID()));
            bottleCap.setLastPreviewLinkUpdate(LocalDateTime.now());
            bottleCapService.addBottleCap(bottleCap);
            counter.getAndIncrement();
        });
        logger.info("Updated " + counter + " locations");
    }
}
