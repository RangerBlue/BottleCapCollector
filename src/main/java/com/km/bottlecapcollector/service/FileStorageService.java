package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.exception.FileStorageException;
import com.km.bottlecapcollector.exception.MyFileNotFoundException;
import com.km.bottlecapcollector.model.BottleCap;
import com.km.bottlecapcollector.model.CapItem;
import com.km.bottlecapcollector.util.HistogramResult;
import com.km.bottlecapcollector.property.CustomProperties;
import com.km.bottlecapcollector.util.BottleCapPair;
import com.km.bottlecapcollector.util.ImageHistogramUtil;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileStorageService {

    private final ImageHistogramUtil imageHistogramUtil = new ImageHistogramUtil();

    @Autowired
    private CustomProperties customProperties;

    public Path fileStorageLocation;
    public Path objectStorageLocation;


    @PostConstruct
    public void setupLocations() {
        fileStorageLocation = Paths.get(customProperties.getUploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
        log.debug("Directory " + fileStorageLocation + " has been created or already exists");
    }

    public long countAllFiles() {
        try {
            return Files.list(fileStorageLocation).count();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<File> getAllPictures() {
        try {
            return Files.walk(fileStorageLocation).map(p -> p.toFile()).skip(1).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long processAllFiles() {
        AtomicInteger amount = new AtomicInteger();
        try {
            Files.walk(fileStorageLocation).filter(p -> p.toFile().isFile()).forEach(file -> {
                imageHistogramUtil.calculateAndStoreHistogram(file.getFileName().toString(), fileStorageLocation, objectStorageLocation);
                amount.getAndIncrement();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return amount.intValue();
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new MyFileNotFoundException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MyFileNotFoundException("File not found " + fileName, ex);
        }
    }
}
