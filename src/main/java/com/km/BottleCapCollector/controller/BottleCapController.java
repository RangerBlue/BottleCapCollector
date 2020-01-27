package com.km.BottleCapCollector.controller;

import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.service.BottleCapService;
import com.km.BottleCapCollector.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
        cap.setPath(fileStorageService.storeFile(file));
        bottleCapService.addBottleCap(cap);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/caps")
    public List<BottleCap> getBottleCaps() {
        logger.debug("Entering getBottleCaps method");
        return bottleCapService.getAllBottleCaps();
    }
}
