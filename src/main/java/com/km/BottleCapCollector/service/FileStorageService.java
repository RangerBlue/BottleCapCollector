package com.km.BottleCapCollector.service;

import com.km.BottleCapCollector.exception.FileStorageException;
import com.km.BottleCapCollector.exception.MyFileNotFoundException;
import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.model.HistogramResult;
import com.km.BottleCapCollector.property.CustomProperties;
import com.km.BottleCapCollector.repository.HistogramResultRepository;
import com.km.BottleCapCollector.util.BottleCapMat;
import com.km.BottleCapCollector.util.BottleCapPair;
import com.km.BottleCapCollector.util.ImageHistogramUtil;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    @Autowired
    private HistogramResultRepository histogramResultRepository;

    @Autowired
    private ImageHistogramUtil imageHistogramUtil;

    @Autowired
    private CustomProperties customProperties;

    public Path fileStorageLocation;
    public Path temporaryFileStorageLocation;
    public Path objectStorageLocation;

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @PostConstruct
    public void setupLocations() {
        fileStorageLocation = Paths.get(customProperties.getUploadDir())
                .toAbsolutePath().normalize();
        objectStorageLocation = Paths.get(customProperties.getObjectDir())
                .toAbsolutePath().normalize();
        temporaryFileStorageLocation = Paths.get(customProperties.getTemporaryDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(fileStorageLocation);
            Files.createDirectories(objectStorageLocation);
            Files.createDirectories(temporaryFileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
        logger.debug("Directory " + fileStorageLocation + " has been created or already exists");
        logger.debug("Directory " + objectStorageLocation + " has been created or already exists");
        logger.debug("Directory " + temporaryFileStorageLocation + " has been created or already exists");
    }

    public String storeFileInPicturesFolder(MultipartFile file) {
        return storeFile(file, fileStorageLocation, true);
    }

    public String storeFileInTemporaryPicturesFolder(MultipartFile file) {
        return storeFile(file, temporaryFileStorageLocation, false);
    }

    private String storeFile(MultipartFile file, Path fileStorageLocation, boolean saveMath) {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (fileName.contains("..")) {
                throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
            }

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            if(saveMath){
                imageHistogramUtil.calculateAndStoreHistogram(fileName, fileStorageLocation, objectStorageLocation);
            }

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    public void calculateAndStoreMathObject(String fileName) {
        imageHistogramUtil.calculateAndStoreHistogram(fileName, fileStorageLocation, objectStorageLocation);
    }
    public Mat calculateAndReturnMathObject(String fileName) {
        return imageHistogramUtil.calculateHistogram(fileName, temporaryFileStorageLocation);
    }

    public BottleCapMat calculateAndReturnMathObjectAsBottleCapMat(MultipartFile file) throws IOException {
        Mat mat = imageHistogramUtil.calculateHistogram(file);
        return imageHistogramUtil.convertMatToBottleCapMat(mat);
    }

    public void calculateEachWithEachCap(List<BottleCap> caps) {
            List<BottleCapPair> dataToProcess = calculateEachWithEach(caps);
            dataToProcess.stream().parallel().forEach(pair -> histogramResultRepository.save(prepareHistogram(pair)));
    }

    public HistogramResult prepareHistogram(BottleCapPair pair) {
        Mat histFromFile1 = imageHistogramUtil.loadMat(pair.getFirstCap().getCapName(), objectStorageLocation);
        Mat histFromFile2 = imageHistogramUtil.loadMat(pair.getSecondCap().getCapName(), objectStorageLocation);
        HistogramResult result = imageHistogramUtil.calculateCoefficients(histFromFile1, histFromFile2);
        result.setFirstCap(pair.getFirstCap());
        result.setSecondCap(pair.getSecondCap());
        return result;
    }

    public HistogramResult prepareHistogram(Mat firstCapMat, BottleCapPair pair) {
        Mat histFromFile1 = firstCapMat;
        Mat histFromFile2 = imageHistogramUtil.loadMat(pair.getSecondCap().getCapName(), objectStorageLocation);
        HistogramResult result = imageHistogramUtil.calculateCoefficients(histFromFile1, histFromFile2);
        result.setFirstCap(pair.getFirstCap());
        result.setSecondCap(pair.getSecondCap());
        return result;
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

    public List<HistogramResult> calculateOneAgainstAllCaps(BottleCap cap, Mat mat, List<BottleCap> inputList) {
        List<BottleCapPair> outputList = new ArrayList<>();
        inputList.stream().parallel().forEach(listCap -> outputList.add(new BottleCapPair(cap, listCap)));
        List<HistogramResult> histogramList = outputList.stream().map(pair ->prepareHistogram(mat,pair)).collect(Collectors.toList());
        return histogramList;
    }

    public List<BottleCapPair> calculateEachWithEach(List<BottleCap> inputList) {
        int inputListSize = inputList.size();
        int outputListSize = (inputList.size() * (inputList.size() - 1) / 2);
        List<BottleCapPair> outputList = new ArrayList<>(outputListSize);
        for (int i = 1; i < inputListSize; i++) {
            outputList.addAll(calculateEach(inputList.subList(0, i + 1), i));
        }
        return outputList;
    }

    private List<BottleCapPair> calculateEach(List<BottleCap> inputList, int size) {
        List<BottleCapPair> result = new ArrayList<>();
        for (int i = 0; i <= size - 1; i++) {
            result.add(new BottleCapPair(inputList.get(i), inputList.get(size)));
        }

        return result;
    }

    public List<HistogramResult> getAllHistogramResults() {
        return (List<HistogramResult>) histogramResultRepository.findAll();
    }
}
