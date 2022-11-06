package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.exception.FileStorageException;
import com.km.bottlecapcollector.property.AppProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@AllArgsConstructor
public class LocalFileStorageService {
    private AppProperties appProperties;

    private static Path fileStorageLocation;

    @PostConstruct
    public void setupLocations() {
        fileStorageLocation = Paths.get(appProperties.getFileUploadDir()).toAbsolutePath().normalize();
    }

    public List<File> getAllPictures() {
        try (Stream<Path> paths = Files.walk(fileStorageLocation)) {
            return paths.map(Path::toFile).skip(1).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error while reading file from local storage, {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    public String getUploadFolderLocation(){
        return fileStorageLocation.toString();
    }
}
