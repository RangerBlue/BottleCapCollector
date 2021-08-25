package com.km.bottlecapcollector.service;

import com.km.bottlecapcollector.exception.FileStorageException;
import com.km.bottlecapcollector.exception.MyFileNotFoundException;
import com.km.bottlecapcollector.model.BottleCap;
import com.km.bottlecapcollector.util.HistogramResult;
import com.km.bottlecapcollector.property.CustomProperties;
import com.km.bottlecapcollector.util.BottleCapMat;
import com.km.bottlecapcollector.util.BottleCapPair;
import com.km.bottlecapcollector.util.ImageHistogramUtil;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class FileStorageService {

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
        log.debug("Directory " + fileStorageLocation + " has been created or already exists");
    }

    public BottleCapMat calculateAndReturnMathObjectAsBottleCapMat(MultipartFile file) throws IOException {
        log.info("Entering calculateAndReturnMathObjectAsBottleCapMat method with multipart file ");
        Mat mat = imageHistogramUtil.calculateHistogram(file);
        return convertMathObjectToBottleCapMat(mat);
    }

    public BottleCapMat convertMathObjectToBottleCapMat(Mat mat) throws IOException {
        log.info("Entering convertMathObjectToBottleCapMat method with multipart file ");
        return imageHistogramUtil.convertMatToBottleCapMat(mat);
    }

    public Mat calculateAndReturnMathObject(MultipartFile file) throws IOException {
        log.info("Entering calculateAndReturnMathObject method with multipart file ");
        return imageHistogramUtil.calculateHistogram(file);
    }

    public List<HistogramResult> calculateEachWithEachCap(List<BottleCap> caps) {
        List<BottleCapPair> dataToProcess = calculateEachWithEach(caps);
        log.info("Processed " + caps.size() + " caps with " + dataToProcess.size() + " pair output");
        List<HistogramResult> results = dataToProcess.stream().parallel().map(bottleCapPair -> prepareHistogram(bottleCapPair)).collect(Collectors.toList());
        log.info("Created " + results.size() + " histogram results");
        return results;
    }

    public double calculateIntersectionMethod(Mat mat) {
        return imageHistogramUtil.calculateIntersection(mat);
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
            log.info("Exception occurred during preparing histogram: " + e.getStackTrace());
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
            log.info("Exception occurred during conversion : " + e.getStackTrace());
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
        log.info("Entering calculateOneAgainstAllCaps method with cap with ID : " + cap.getId() + " and name "
                + cap.getCapName() + " against " + inputList.size() + " items");
        log.info("Creating BottleCap pairs");
        List<BottleCapPair> outputList = inputList.stream().parallel().map(bottleCap -> new BottleCapPair(cap, bottleCap)).collect(Collectors.toList());
        log.info("Preparing histograms");
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
