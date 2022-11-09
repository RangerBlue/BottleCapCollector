package com.km.bottlecapcollector.service;


import com.km.bottlecapcollector.dto.BottleCapDto;
import com.km.bottlecapcollector.dto.BottleCapValidationResponseDto;
import com.km.bottlecapcollector.dto.CapPictureDto;
import com.km.bottlecapcollector.exception.CapNotFoundException;
import com.km.bottlecapcollector.exception.ImageSignatureException;
import com.km.bottlecapcollector.exception.ImageUploaderException;
import com.km.bottlecapcollector.google.ImageUploader;
import com.km.bottlecapcollector.model.*;
import com.km.bottlecapcollector.opencv.HistogramResult;
import com.km.bottlecapcollector.opencv.ImageHistogramUtil;
import com.km.bottlecapcollector.repository.CapItemRepository;
import com.km.bottlecapcollector.util.*;
import lombok.extern.slf4j.Slf4j;
import com.km.bottlecapcollector.mapper.BottleCapMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BottleCapService implements SimilarityCalculator{


    @Autowired
    private CapItemRepository capItemRepository;

    @Autowired
    private ImageUploader imageUploaderService;

    @Autowired
    private ComparisonRangeService comparisonRangeService;

    private final BottleCapMapper mapper = BottleCapMapper.INSTANCE;

    ItemFactory itemFactory = new ItemFactoryImpl();

    @Caching(evict =
    @CacheEvict(value = "caps", allEntries = true))
    public CapItem addBottleCap(CapItem cap) {
        return capItemRepository.save(cap);
    }


    @Cacheable(value = "caps")
    public List<CapItem> getAllCapItems() {
        log.trace("Retrieving all caps items from database");
        return (List<CapItem>) capItemRepository.findAll();
    }

    public List<CapPictureDto> getAllBottleCapsLinks() {
        return getAllCapItems().stream().map(mapper::capItemToCapPictureDto)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<BottleCapDto> getAllBottleCapsDto() {
        return getAllCapItems().stream().map(mapper::capItemToBottleCapDto).collect(Collectors.toList());
    }


    @Transactional
    @Caching(evict =
    @CacheEvict(value = "caps", allEntries = true))
    public void removeCapItem(long id) {
        log.info("Removing cap item with id: {}", id);
        CapItem capItem = getCapItem(id);
        log.info("Removing cap item with name: {}", capItem.getName());
        String imageProviderID = capItem.getImage().getProvider().getImageProviderId();
        log.info("Removing googledrive item picture id: {}", imageProviderID);
        capItemRepository.delete(capItem);
        imageUploaderService.deleteFile(imageProviderID);
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
       return mapper.capItemToBottleCapDto(item);
    }

    public BottleCapDto getCapItemDto(long id) {
        return capItemToBottleCapDto(getCapItem(id));
    }

    public CapItem getCapItem(long id) {
        return capItemRepository.findById(id).orElseThrow(() -> new CapNotFoundException(id));
    }

    @Transactional
    public Long addCapItem(String itemName, String description, MultipartFile file) {
        String fileName = file.getOriginalFilename();
        log.info("Adding new cap item: {}, from picture: {}", itemName, fileName);
        CapItem capItem = itemFactory.getCapItem();
        capItem.setName(itemName);
        capItem.setDescription(description);
        AbstractImage image = capItem.getImage();
        AbstractSignature openCVImageSignature = image.getSignature();
        String uploadedImageUrl;
        try {
            openCVImageSignature.calculateParameters(file);

            AbstractImageProvider googleDriveProvider = capItem.getImage().getProvider();
            uploadedImageUrl = googleDriveProvider.upload(file, imageUploaderService);
            image.setUrl(uploadedImageUrl);
            googleDriveProvider.setFileName(fileName);
        } catch (ImageSignatureException ise) {
            log.error("Error during calculating image signature parameters at: {}", itemName);
            throw new ImageSignatureException(ise);
        } catch (ImageUploaderException iue) {
            log.error("Error during upload image to external service: {}", itemName);
            throw new ImageSignatureException(iue);
        }

        log.info("Picture has been successfully uploaded at: {}", uploadedImageUrl);

        return capItemRepository.save(capItem).getId();
    }

    public BottleCapValidationResponseDto validateCapItem(String itemName, MultipartFile file) throws ImageSignatureException {
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

    @Override
    public SimilarityModel calculateSimilarityModel(String itemName, MultipartFile file, int resultSize) throws ImageSignatureException{
        log.info("Calculating size {} similarity model for: {}", resultSize, itemName);
        List<CapItem> caps = getAllCapItems();
        CapItem candidateCap = itemFactory.getCapItem();
        candidateCap.setName(itemName);
        AbstractImage image = candidateCap.getImage();
        AbstractSignature openCVImageSignature = image.getSignature();
        openCVImageSignature.calculateParameters(file);

        List<HistogramResult> histogramResults = ImageHistogramUtil.calculateOneAgainstAllCaps(candidateCap, caps);
        return comparisonRangeService.calculateSimilarityModelForCap(histogramResults,
                resultSize);
    }

    public BottleCapDto validateWhatCapYouAre(String itemName, MultipartFile file) throws ImageSignatureException{
        log.info("Entering validateWhatCapYouAre method for {}", itemName);
        SimilarityModel similarityModel = calculateSimilarityModel(itemName, file, SimilarityModel.similarCapAmountOne);
        CapItem capItem = similarityModel.getSimilarCaps().stream().map(HistogramResult::getSecondCap).findFirst().
                orElse(new CapItem());
        return capItemToBottleCapDto(capItem);
    }
}
