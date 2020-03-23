package com.km.BottleCapCollector.service;

import com.km.BottleCapCollector.exception.FileStorageException;
import com.km.BottleCapCollector.exception.MyFileNotFoundException;
import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.model.HistogramResult;
import com.km.BottleCapCollector.property.CustomProperties;
import com.km.BottleCapCollector.repository.HistogramResultRepository;
import com.km.BottleCapCollector.util.ImageHistogramFactory;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    @Autowired
    private HistogramResultRepository histogramResultRepository;

    @Autowired
    private ImageHistogramFactory imageHistogramFactory;

    private final Path fileStorageLocation;
    private final Path objectStorageLocation;

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Autowired
    public FileStorageService(CustomProperties customProperties) {
        fileStorageLocation = Paths.get(customProperties.getUploadDir())
                .toAbsolutePath().normalize();
        objectStorageLocation = Paths.get(customProperties.getObjectDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(fileStorageLocation);
            Files.createDirectories(objectStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
        logger.debug("Directory "+ fileStorageLocation + " has been created or already exists");
        logger.debug("Directory "+ objectStorageLocation + " has been created or already exists");

    }

    public String storeFile(MultipartFile file) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            imageHistogramFactory.calculateAndStoreHistogram(fileName, fileStorageLocation, objectStorageLocation);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public void calculateAndStoreMathObject(String fileName){
        imageHistogramFactory.calculateAndStoreHistogram(fileName, fileStorageLocation, objectStorageLocation);
    }

    public void calculateEachWithEachCap(List<BottleCap> caps){
        try {
            List<File> fileList = Files.walk(objectStorageLocation).map(path -> path.toFile()).skip(1).limit(2).collect(Collectors.toList());
            Mat histFromFile1 = imageHistogramFactory.loadMat(fileList.get(0).getName(), objectStorageLocation);
            Mat histFromFile2 = imageHistogramFactory.loadMat(fileList.get(1).getName(), objectStorageLocation);
            HistogramResult result = imageHistogramFactory.calculateCoefficients(histFromFile1, histFromFile2);
            result.setFirstCap(caps.get(0));
            result.setSecondCap(caps.get(1));
            histogramResultRepository.save(result);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long countAllFiles(){
        try {
            return Files.list(fileStorageLocation).count();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public List<File> getAllPictures(){
        try {
            return Files.walk(fileStorageLocation).map(p->p.toFile()).skip(1).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long processAllFiles(){
        AtomicInteger amount = new AtomicInteger();
        try {
            Files.walk(fileStorageLocation).filter(p->p.toFile().isFile()).forEach(file ->{
                imageHistogramFactory.calculateAndStoreHistogram(file.getFileName().toString(), fileStorageLocation, objectStorageLocation);
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
