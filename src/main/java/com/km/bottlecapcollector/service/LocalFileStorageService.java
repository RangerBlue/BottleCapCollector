package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.exception.FileStorageException;
import com.km.bottlecapcollector.property.CustomProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalFileStorageService {
    @Autowired
    private CustomProperties customProperties;

    public Path fileStorageLocation;


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
}
