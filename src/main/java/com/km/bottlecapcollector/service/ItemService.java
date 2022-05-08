package com.km.bottlecapcollector.service;


import com.km.bottlecapcollector.dto.BottleCapDto;
import com.km.bottlecapcollector.dto.BottleCapValidationResponseDto;
import com.km.bottlecapcollector.exception.CapNotFoundException;
import com.km.bottlecapcollector.exception.GoogleDriveException;
import com.km.bottlecapcollector.google.GoogleDriveService;
import com.km.bottlecapcollector.model.*;
import com.km.bottlecapcollector.repository.BottleCapRepository;
import com.km.bottlecapcollector.repository.CapItemRepository;
import com.km.bottlecapcollector.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ItemService {

    @Autowired
    private BottleCapRepository repository;

    @Autowired
    private CapItemRepository capItemRepository;

    @Autowired
    private GoogleDriveService googleDriveService;

    @Autowired
    private ComparisonRangeService comparisonRangeService;

    ItemFactory itemFactory = new ItemFactoryImpl();

    @Caching(evict =
    @CacheEvict(value = "caps", allEntries = true))
    public BottleCap addBottleCap(BottleCap cap) {
        return repository.save(cap);
    }

    @Cacheable(value = "caps")
    public List<BottleCap> getAllBottleCaps() {
        log.trace("Retrieving all caps from database");
        return (List<BottleCap>) repository.findAll();
    }

    public BottleCap getBottleCap(long id) {
        return repository.findById(id).orElseThrow(() -> new CapNotFoundException(id));
    }

    public List<CapItem> getAllCapItems() {
        log.trace("Retrieving all caps items from database");
        return (List<CapItem>) capItemRepository.findAll();
    }

    public CapItem removeCapItem(long id) {
        log.info("Removing cap item with id: {}", id);
        CapItem capItem = getCapItem(id);
        String imageProviderID = capItem.getImage().getProvider().getImageProviderId();
        log.info("Removing googledrive item with id: {}", imageProviderID);
        googleDriveService.deleteFile(imageProviderID);
        capItemRepository.delete(capItem);
        return capItem;
    }

    public BottleCapDto updateCapItemDto(long id, String newName, String newDesc) {
        return capItemToBottleCapDto(updateCapItem(id, newName, newDesc));
    }

    public CapItem updateCapItem(long id, String newName, String newDesc) {
        log.info("Updating cap item {} with new: name {} and description {}", id, newDesc, newDesc);
        CapItem capItem = getCapItem(id);
        log.info("Current name {} and description {}", capItem.getName(), capItem.getDescription());
        capItem.setName(newName);
        capItem.setDescription(newDesc);
        capItemRepository.save(capItem);
        return capItem;
    }

    public BottleCapDto capItemToBottleCapDto(CapItem item) {
        BottleCapDto dto = new BottleCapDto();
        AbstractImage image = item.getImage();
        OpenCVImageSignature openCVImageSignature = (OpenCVImageSignature) image.getSignature();
        AbstractImageProvider googleDriveProvider = image.getProvider();

        dto.setId(image.getId());
        dto.setFileLocation(image.getUrl());
        dto.setGoogleDriveID(googleDriveProvider.getImageProviderId());
        dto.setCapName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setCreationDate(item.getCreateDateTime().toString());
        dto.setCols(openCVImageSignature.getImageCols());
        dto.setRows(openCVImageSignature.getImageRows());
        dto.setIntersectionValue(openCVImageSignature.getIntersectionValue());
        dto.setLastPreviewLinkUpdate(googleDriveProvider.getUpdateDateTime().toString());
        return dto;
    }

    public BottleCapDto getCapItemDto(long id) {
        return capItemToBottleCapDto(getCapItem(id));
    }

    public CapItem getCapItem(long id) {
        return capItemRepository.findById(id).orElseThrow(() -> new CapNotFoundException(id));
    }

    public Long addCapItem(String itemName, String description, MultipartFile file) {
        log.info("Adding new cap item: {}", itemName);
        CapItem capItem;
        try {
            capItem = itemFactory.getCapItem();
            capItem.setName(itemName);
            capItem.setDescription(description);
            AbstractImage image = capItem.getImage();
            OpenCVImageSignature openCVImageSignature = (OpenCVImageSignature) image.getSignature();
            openCVImageSignature.calculateParameters(file);
            GoogleDriveProvider googleDriveProvider = (GoogleDriveProvider) capItem.getImage().getProvider();
            image.setUrl(googleDriveProvider.upload(file, googleDriveService));

        } catch (IOException e) {
            throw new GoogleDriveException(e);
        }

        return capItemRepository.save(capItem).getId();
    }

    public BottleCapValidationResponseDto validateCapItem(String itemName, MultipartFile file) throws IOException {
        log.info("Entering validateCapItem method for {}", itemName);
        SimilarityModel similarityModel = calculateSimilarityModel(itemName, file, SimilarityModel.similarCapAmount);
        ArrayList<CapItem> similarCaps = similarityModel.getSimilarCaps().stream().map(HistogramResult::getSecondCap).
                collect(Collectors.toCollection(ArrayList::new));
        ArrayList<Long> similarCapsIDs = similarCaps.stream().map(CapItem::getId).
                collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> similarCapsURLs = similarCaps.stream().map(item -> item.getImage().getUrl()).
                collect(Collectors.toCollection(ArrayList::new));
        return new BottleCapValidationResponseDto(similarityModel.isDuplicate(), similarCapsIDs, similarCapsURLs,
                similarityModel.getSimilarityDistribution());
    }

    public SimilarityModel calculateSimilarityModel(String itemName, MultipartFile file, int resultSize) throws IOException {
        List<CapItem> caps = getAllCapItems();
        CapItem candidateCap = itemFactory.getCapItem();
        candidateCap.setName(itemName);
        AbstractImage image = candidateCap.getImage();
        OpenCVImageSignature openCVImageSignature = (OpenCVImageSignature) image.getSignature();
        openCVImageSignature.calculateParameters(file);

        List<HistogramResult> histogramResults = ImageHistogramUtil.calculateOneAgainstAllCaps(candidateCap, caps);
        return comparisonRangeService.calculateSimilarityModelForCap(histogramResults,
                resultSize);
    }

    public BottleCapDto validateWhatCapYouAre(String itemName, MultipartFile file) throws IOException {
        log.info("Entering validateWhatCapYouAre method for {}", itemName);
        SimilarityModel similarityModel = calculateSimilarityModel(itemName, file, SimilarityModel.similarCapAmountOne);
        CapItem capItem = similarityModel.getSimilarCaps().stream().map(HistogramResult::getSecondCap).findFirst().
                orElse(new CapItem());
        return capItemToBottleCapDto(capItem);
    }

    @Caching(evict =
    @CacheEvict(value = "caps", allEntries = true))
    public void deleteBottleCapWithId(Long id) {
        repository.deleteById(id);
    }

    @Profile("admin")
    public void addBottleCapForInitialUpload(File file) {
        repository.save(new BottleCap(file.getName(), file.getName()));
    }

}
