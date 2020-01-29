package com.km.BottleCapCollector.controller;

import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.payload.UploadFileResponse;
import com.km.BottleCapCollector.service.BottleCapService;
import com.km.BottleCapCollector.service.FileStorageService;
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
import java.io.IOException;
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

    @PostMapping("/addCap")
    public ResponseEntity addBottleCap(String capName, @RequestParam("file") MultipartFile file) {
        logger.debug("Entering addBottleCap method");
        BottleCap cap = new BottleCap(capName);
        if(bottleCapService.isDuplicate(cap)){
            return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);

        }
        cap.setPath(uploadFile(file).getFileName());
        bottleCapService.addBottleCap(cap);
        return new ResponseEntity<>(HttpStatus.CREATED);
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
        String fileName = fileStorageService.storeFile(file);


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
        return downloadFile(cap.getPath(), request);
    }

}
