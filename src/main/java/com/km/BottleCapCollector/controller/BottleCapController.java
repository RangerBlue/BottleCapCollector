package com.km.BottleCapCollector.controller;

import com.km.BottleCapCollector.exception.FileStorageException;
import com.km.BottleCapCollector.google.GoogleDriveService;
import com.km.BottleCapCollector.google.GoogleDriveUploadItem;
import com.km.BottleCapCollector.model.*;
import com.km.BottleCapCollector.payload.ValidateBottleCapResponse;
import com.km.BottleCapCollector.service.BottleCapService;
import com.km.BottleCapCollector.service.ComparisonRangeService;
import com.km.BottleCapCollector.service.FileStorageService;
import com.km.BottleCapCollector.util.BottleCapMat;
import com.km.BottleCapCollector.util.SimilarityModel;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
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

    @PostMapping("/addCap")
    public ResponseEntity<BottleCap> addBottleCap(@RequestParam("name") String capName, @RequestParam("file") MultipartFile file) {
        logger.info("Entering addBottleCap method");
        BottleCap cap = null;
        BottleCapMat matFile = null;
        String fileLocation = "";
        try {
            fileLocation = uploadFileToDrive(file);
            matFile = fileStorageService.calculateAndReturnMathObjectAsBottleCapMat(file);
            cap = new BottleCap(capName, matFile, fileLocation);
            bottleCapService.addBottleCap(cap);
        } catch (IOException e) {
            throw new FileStorageException("Could not store file " + file.getName() + ". Please try again!", e);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/validateCap")
    public ValidateBottleCapResponse validateBottleCap(@RequestParam("name") String capName, @RequestParam("file") MultipartFile file) throws IOException {
        logger.debug("Entering validateBottleCap method");
        List<BottleCap> caps = new ArrayList<>(bottleCapService.getAllBottleCaps());
        Mat mat = fileStorageService.calculateAndReturnMathObject(file);
        BottleCapMat bottleCapMat = fileStorageService.convertMathObjectToBottleCapMat(mat);
        BottleCap savedCap = new BottleCap(capName, bottleCapMat);
        List<HistogramResult> histogramResults = fileStorageService.calculateOneAgainstAllCaps(savedCap, mat, caps);
        SimilarityModel similarityModel = comparisonRangeService.calculateSimilarityModelForCap(histogramResults);
        ArrayList<BottleCap> similarCaps = similarityModel.getSimilarCaps().stream().map(HistogramResult::getSecondCap).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Long> similarCapsIDs = similarCaps.stream().map(BottleCap::getId).collect(Collectors.toCollection(ArrayList::new));
        return new ValidateBottleCapResponse(similarityModel.isDuplicate(), similarCapsIDs);
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

    @PostMapping("/uploadFileToDrive")
    public String uploadFileToDrive(@RequestParam("file") MultipartFile multipartFile) throws IOException {

        String contentType = multipartFile.getContentType();
        String originalFilename = multipartFile.getOriginalFilename();
        byte[] byteArray = multipartFile.getBytes();
        String fileName = multipartFile.getName();
        GoogleDriveUploadItem uploadItem = new GoogleDriveUploadItem(contentType, originalFilename, fileName, byteArray);
        return googleDriveService.uploadFile(uploadItem);
    }

    @GetMapping(value = "/capDrive/{id}")
    public String getFile(@PathVariable String id) throws GeneralSecurityException, IOException {
        return googleDriveService.getFile(id);

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
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                MultipartFile multipartFile = new MockMultipartFile(file.getName(), file.getName(),
                        ContentType.IMAGE_JPEG.toString(), fileInputStream);
                addBottleCap(file.getName(), multipartFile);
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
