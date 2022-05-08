package com.km.bottlecapcollector.controller;

import com.km.bottlecapcollector.dto.*;
import com.km.bottlecapcollector.google.GoogleDriveService;
import com.km.bottlecapcollector.model.*;
import com.km.bottlecapcollector.service.ItemService;
import com.km.bottlecapcollector.service.ComparisonRangeService;
import com.km.bottlecapcollector.service.FileStorageService;
import com.km.bottlecapcollector.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.modelmapper.ModelMapper;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
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
@Slf4j
public class BottleCapController {

    private final ModelMapper modelMapper;
    private final ItemService itemService;
    private final FileStorageService fileStorageService;
    private final ComparisonRangeService comparisonRangeService;
    private final GoogleDriveService googleDriveService;

    public BottleCapController(CustomMapper modelMapper,
                               ItemService itemService,
                               FileStorageService fileStorageService,
                               ComparisonRangeService comparisonRangeService,
                               GoogleDriveService googleDriveService) {
        this.modelMapper = modelMapper;
        this.itemService = itemService;
        this.fileStorageService = fileStorageService;
        this.comparisonRangeService = comparisonRangeService;
        this.googleDriveService = googleDriveService;
    }

    @PostMapping("/caps")
    public ResponseEntity<Long> addBottleCap(@RequestParam("name") String capName,
                                             @RequestParam("desc") String description,
                                             @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.addCapItem(capName, description, file));
    }


    @DeleteMapping("/caps/{id}")
    public ResponseEntity<String> deleteBottleCap(@PathVariable Long id) {
        itemService.removeCapItem(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/caps/{id}")
    public ResponseEntity<BottleCapDto> getBottleCap(@PathVariable Long id) {
        return ResponseEntity.ok().body(itemService.getCapItemDto(id));
    }

    @PutMapping("/caps/{id}")
    public ResponseEntity<BottleCapDto> updateCap(@PathVariable Long id, @RequestParam("newName") String newName,
                                               @RequestParam("newDesc") String newDesc) {
        return ResponseEntity.ok().body(itemService.updateCapItemDto(id, newName, newDesc));
    }

    @PostMapping("/validateCap")
    public BottleCapValidationResponseDto validateBottleCap(@RequestParam("name") String capName, MultipartFile file)
            throws IOException {
        return itemService.validateCapItem(capName, file);
    }

    @PostMapping("/whatCapAreYou")
    public ResponseEntity<BottleCapDto> whatCapAreYou(@RequestParam("name") String capName, MultipartFile file)
            throws IOException {
        return ResponseEntity.ok().body(itemService.validateWhatCapYouAre(capName, file));
    }


    @GetMapping("/caps")
    public List<BottleCapDto> getBottleCaps() {
        log.trace("Retrieving all caps");
        return itemService.getAllBottleCaps().stream().
                map(bottleCap -> modelMapper.map(bottleCap, BottleCapDto.class)).collect(Collectors.toList());
    }

    @GetMapping("/links")
    public List<BottleCapPictureDto> getBottleCapsLinks() {
        log.info("Entering getBottleCapsLinks method");
        //TODO: Change it to new structure
        return itemService.getAllBottleCaps().stream().map(bottleCap ->
                modelMapper.map(bottleCap, BottleCapPictureDto.class))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @GetMapping("/catalog")
    public ArrayList<BottleCapCatalogDto> getCapCatalog() {
        log.info("Entering getCapCatalog method");
        //TODO: Change it to new structure
        return itemService.getAllBottleCaps().stream().map(bottleCap ->
                new BottleCapCatalogDto(bottleCap.getId(), bottleCap.getFileLocation(), bottleCap.getCapName(),
                        bottleCap.getDescription()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @PostMapping("/admin/uploadFileToDrive")
    public String uploadFileToDrive(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        return googleDriveService.uploadFile(multipartFile);
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
        List<CapItem> caps = itemService.getAllCapItems();
        List<HistogramResult> histogramResults = ImageHistogramUtil.calculateEachWithEachCap(caps);
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
        //TODO: Change it to new structure
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
        //TODO: Change it to new structure
        log.info("Entering updateCapLocations method");
        List<BottleCap> caps = new ArrayList<>(itemService.getAllBottleCaps());
        AtomicInteger counter = new AtomicInteger();
        caps.forEach(bottleCap -> {
            bottleCap.setFileLocation(googleDriveService.getFileUrl(bottleCap.getGoogleDriveID()));
            bottleCap.setLastPreviewLinkUpdate(LocalDateTime.now());
            itemService.addBottleCap(bottleCap);
            counter.getAndIncrement();
        });
        log.info("Updated " + counter + " locations");
    }
}
