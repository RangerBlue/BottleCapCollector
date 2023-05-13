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
import com.km.bottlecapcollector.property.AppProperties;
import com.km.bottlecapcollector.repository.CapItemRepository;
import com.km.bottlecapcollector.util.*;
import com.km.bottlecapcollector.util.color.HSBColor;
import com.km.bottlecapcollector.util.color.HSBColorRange;
import com.km.bottlecapcollector.util.color.HSBColorService;
import lombok.extern.slf4j.Slf4j;
import com.km.bottlecapcollector.mapper.BottleCapMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BottleCapService implements SimilarityCalculator{


    private final CapItemRepository capItemRepository;

    private final ImageUploader imageUploaderService;
    private final ComparisonRangeService comparisonRangeService;

    private final AppProperties appProperties;

    private final BottleCapMapper mapper = BottleCapMapper.INSTANCE;

    ItemFactory itemFactory = new ItemFactoryImpl();

    public BottleCapService(CapItemRepository capItemRepository, ImageUploader imageUploaderService,
                            ComparisonRangeService comparisonRangeService, AppProperties appProperties) {
        this.capItemRepository = capItemRepository;
        this.imageUploaderService = imageUploaderService;
        this.comparisonRangeService = comparisonRangeService;
        this.appProperties = appProperties;
    }

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";
    public static final String DEFAULT_TEXT= "";

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

    public List<CapItem> getCapsWithSimilarColors(HSBColorRange colorRange) {
        int responseSize = appProperties.getSimilaritySearchResponseSize();
        log.trace("Retrieving {}} caps items from database based on colors", responseSize);
        return capItemRepository.findByColorsValues(
                colorRange.getHueRange().getMoreThan(),
                colorRange.getHueRange().getPivotLessThan(),
                colorRange.getHueRange().getPivotMoreThan(),
                colorRange.getHueRange().getLessThan(),
                colorRange.getSaturationRange().getMoreThan(),
                colorRange.getSaturationRange().getPivotLessThan(),
                colorRange.getSaturationRange().getPivotMoreThan(),
                colorRange.getSaturationRange().getLessThan(),
                colorRange.getBrightnessRange().getMoreThan(),
                colorRange.getBrightnessRange().getPivotLessThan(),
                colorRange.getBrightnessRange().getPivotMoreThan(),
                colorRange.getBrightnessRange().getLessThan(),PageRequest.of(0, responseSize));
    }

    public List<CapPictureDto> findCapsPaginated(int pageNo, int pageSize, String sortBy, String sortDir) {
        Page<CapItem> pagedResult = getAllCapsByPageable(pageNo, pageSize, sortBy, sortDir);
        return pagedResult.stream().map(mapper::capItemToCapPictureDto).collect(Collectors.toList());
    }

    public List<CapPictureDto> getAllBottleCapsLinks() {
        return getAllCapItems().stream().map(mapper::capItemToCapPictureDto)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<BottleCapDto> getAllBottleCapsDto() {
        return getAllCapItems().stream().map(mapper::capItemToBottleCapDto).collect(Collectors.toList());
    }
    private Page<CapItem> getAllCapsByPageable(int pageNo, int pageSize, String sortBy, String sortDir){

        return capItemRepository.findAll(getBottleCapPageable(pageNo, pageSize, sortBy, sortDir));
    }

    private Pageable getBottleCapPageable(int pageNo, int pageSize, String sortBy, String sortDir){
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(pageNo, pageSize, sort);
    }


    public List<BottleCapDto> findCapByText(int pageNo, int pageSize, String sortBy, String sortDir, String text) {
        Pageable pageable = getBottleCapPageable(pageNo, pageSize, sortBy, sortDir);
        final Page<CapItem> byNameOrDescriptionContaining = capItemRepository.findByNameContainingOrDescriptionContainingAllIgnoreCase(text, text, pageable);
        return byNameOrDescriptionContaining.stream().map(mapper::capItemToBottleCapDto).collect(Collectors.toList());
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
        HSBColor hsbColor = HSBColorService.calculateColor(file);
        image.setHue(hsbColor.getHue());
        image.setSaturation(hsbColor.getSaturation());
        image.setBrightness(hsbColor.getBrightness());
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
        SimilarityModel similarityModel = calculateSimilarityModel(itemName, file, appProperties.getValidationResponseItemSize());
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
        StopWatch measurement = new StopWatch();
        HSBColor hsbColor = HSBColorService.calculateColor(file);
        HSBColorRange hsbColorRange = HSBColorService.calculateRanges(hsbColor);

        measurement.start("Database call ");
        List<CapItem> caps = getCapsWithSimilarColors(hsbColorRange);
        if(caps.size() < appProperties.getValidationResponseItemSize()){
            log.warn("Did not found sufficient amount of items ({}), extending range..", caps.size());
            HSBColorRange hsbColorRangeExtended = HSBColorService.calculateRangesExtended(hsbColor);
            caps = getCapsWithSimilarColors(hsbColorRangeExtended);
        }

        measurement.stop();

        log.info(measurement.getLastTaskName() + "took " + measurement.getTotalTimeSeconds() + " s");

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
