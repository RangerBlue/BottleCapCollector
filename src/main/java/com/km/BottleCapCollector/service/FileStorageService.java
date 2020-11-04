package com.km.BottleCapCollector.service;

import com.km.BottleCapCollector.exception.FileStorageException;
import com.km.BottleCapCollector.exception.MyFileNotFoundException;
import com.km.BottleCapCollector.model.BottleCap;
import com.km.BottleCapCollector.util.HistogramResult;
import com.km.BottleCapCollector.property.CustomProperties;
import com.km.BottleCapCollector.util.BottleCapMat;
import com.km.BottleCapCollector.util.BottleCapPair;
import com.km.BottleCapCollector.util.ImageHistogramUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private static final Logger logger = LogManager.getLogger(FileStorageService.class);

    @Autowired
    private ImageHistogramUtil imageHistogramUtil;

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
        logger.debug("Directory " + fileStorageLocation + " has been created or already exists");
    }

    public BottleCapMat calculateAndReturnMathObjectAsBottleCapMat(MultipartFile file) throws IOException {
        logger.info("Entering calculateAndReturnMathObjectAsBottleCapMat method with multipart file ");
        Mat mat = imageHistogramUtil.calculateHistogram(file);
        return convertMathObjectToBottleCapMat(mat);
    }

    public BottleCapMat convertMathObjectToBottleCapMat(Mat mat) throws IOException {
        logger.info("Entering convertMathObjectToBottleCapMat method with multipart file ");
        return imageHistogramUtil.convertMatToBottleCapMat(mat);
    }

    public Mat calculateAndReturnMathObject(MultipartFile file) throws IOException {
        logger.info("Entering calculateAndReturnMathObject method with multipart file ");
        return imageHistogramUtil.calculateHistogram(file);
    }

    public List<HistogramResult> calculateEachWithEachCap(List<BottleCap> caps) {
        List<BottleCapPair> dataToProcess = calculateEachWithEach(caps);
        logger.info("Processed " + caps.size() + " caps with " + dataToProcess.size() + " pair output");
        List<HistogramResult> results = dataToProcess.stream().parallel().map(bottleCapPair -> prepareHistogram(bottleCapPair)).collect(Collectors.toList());
        logger.info("Created " + results.size() + " histogram results");
        return results;
    }

    public HistogramResult prepareHistogram(BottleCapPair pair) {
        Mat histFromFile1 = null;
        Mat histFromFile2 = null;
        try {
            histFromFile1 = imageHistogramUtil.convertBottleCapMatToMat(new BottleCapMat(pair.getFirstCap().getData(),
                    pair.getFirstCap().getCols(), pair.getFirstCap().getRows()));
            histFromFile2 = imageHistogramUtil.convertBottleCapMatToMat(new BottleCapMat(pair.getSecondCap().getData(),
                    pair.getSecondCap().getCols(), pair.getSecondCap().getRows()));
        } catch (IOException e) {
            logger.info("Exception occurred during preparing histogram: " + e.getStackTrace());
        }

        HistogramResult result = imageHistogramUtil.calculateCoefficients(histFromFile1, histFromFile2);
        result.setFirstCap(pair.getFirstCap());
        result.setSecondCap(pair.getSecondCap());
        return result;
    }

    public HistogramResult prepareHistogram(Mat firstCapMat, BottleCapPair pair) {
        Mat secondCapMat = null;
        try {
            secondCapMat = imageHistogramUtil.convertBottleCapMatToMat(
                    new BottleCapMat(pair.getSecondCap().getData(),
                            pair.getSecondCap().getCols(),
                            pair.getSecondCap().getRows()));
        } catch (IOException e) {
            logger.info("Exception occurred during conversion : " + e.getStackTrace());
        }
        HistogramResult result = imageHistogramUtil.calculateCoefficients(firstCapMat, secondCapMat);
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
        logger.info("Entering calculateOneAgainstAllCaps method with cap with ID : " + cap.getId() + " and name "
                + cap.getCapName() + " against " + inputList.size() + " items");
        List<BottleCapPair> outputList = new ArrayList<>();
        logger.info("Creating BottleCap pairs");
        inputList.stream().parallel().forEach(listCap -> outputList.add(new BottleCapPair(cap, listCap)));
        logger.info("Preparing histograms");
        return outputList.stream().map(pair -> prepareHistogram(mat, pair)).collect(Collectors.toList());
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
}
