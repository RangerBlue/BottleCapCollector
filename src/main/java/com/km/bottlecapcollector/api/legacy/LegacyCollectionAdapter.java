package com.km.bottlecapcollector.api.legacy;

import com.km.bottlecapcollector.api.legacy.mapper.LegacyApiMapper;
import com.km.bottlecapcollector.api.legacy.model.BottleCapDto;
import com.km.bottlecapcollector.api.legacy.model.BottleCapValidationResponseDto;
import com.km.bottlecapcollector.api.legacy.model.CapPictureDto;
import com.km.bottlecapcollector.api.model.request.CreateCollectionItemRequest;
import com.km.bottlecapcollector.api.model.request.UpdateCollectionItem;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import com.km.bottlecapcollector.property.AppProperties;
import com.km.bottlecapcollector.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Adapter service that bridges legacy BottleCapController API with new CollectionService.
 * Handles mapping between legacy DTOs and new response objects using MapStruct,
 * and uses configurable userId and collectionKey from application properties.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LegacyCollectionAdapter {

    private final CollectionService collectionService;
    private final AppProperties appProperties;

    private final LegacyApiMapper mapper = LegacyApiMapper.INSTANCE;

    /**
     * Creates a new cap item using the legacy API format.
     *
     * @param capName     name of the cap
     * @param description description of the cap
     * @param file        image file
     * @return the generated Long ID (parsed from Firestore document ID)
     */
    public Long addCapItem(String capName, String description, MultipartFile file) {
        log.info("Legacy API: Adding cap item: {}", capName);

        CreateCollectionItemRequest request = CreateCollectionItemRequest.builder()
                .name(capName)
                .description(description)
                .userId(getLegacyUserId())
                .collectionName(getLegacyCollectionName())
                .build();

        CollectionItemResponse response = collectionService.createCollectionItem(
                getLegacyCollectionKey(),
                request,
                file
        );

        return mapper.stringToLong(response.getId());
    }

    /**
     * Removes a cap item by its ID.
     *
     * @param id the cap ID
     */
    public void removeCapItem(Long id) {
        log.info("Legacy API: Removing cap item: {}", id);
        collectionService.deleteItem(
                getLegacyCollectionKey(),
                id.toString(),
                getLegacyUserId()
        );
    }

    /**
     * Gets a cap item DTO by its ID.
     *
     * @param id the cap ID
     * @return the cap as BottleCapDto
     */
    public BottleCapDto getCapItemDto(Long id) {
        log.trace("Legacy API: Getting cap item: {}", id);
        CollectionItemResponse response = collectionService.getCollectionItem(
                getLegacyCollectionKey(),
                id.toString(),
                getLegacyUserId()
        );
        return mapper.toBottleCapDto(response);
    }

    /**
     * Updates a cap item.
     *
     * @param id      the cap ID
     * @param newName new name
     * @param newDesc new description
     * @return updated BottleCapDto
     */
    public BottleCapDto updateCapItemDto(Long id, String newName, String newDesc) {
        log.info("Legacy API: Updating cap item: {} with name: {}", id, newName);

        UpdateCollectionItem request = UpdateCollectionItem.builder()
                .name(newName)
                .description(newDesc)
                .build();

        CollectionItemResponse response = collectionService.updateItem(
                getLegacyCollectionKey(),
                id.toString(),
                getLegacyUserId(),
                request
        );

        return mapper.toBottleCapDto(response);
    }

    /**
     * Validates a cap item for potential duplicates.
     *
     * @param itemName name of the item being validated
     * @param file     image file to check
     * @return validation response with similar caps
     */
    public BottleCapValidationResponseDto validateCapItem(String itemName, MultipartFile file) {
        log.info("Legacy API: Validating cap item: {}", itemName);

        ValidateItemResponse response = collectionService.validateItem(
                getLegacyCollectionKey(),
                getLegacyUserId(),
                file
        );

        return mapper.toValidationResponseDto(response);
    }

    /**
     * Finds the most similar cap to the uploaded image.
     *
     * @param itemName name of the item
     * @param file     image file
     * @return the most similar cap as BottleCapDto
     */
    public BottleCapDto validateWhatCapYouAre(String itemName, MultipartFile file) {
        log.info("Legacy API: What cap are you for: {}", itemName);

        ValidateItemResponse response = collectionService.validateItem(
                getLegacyCollectionKey(),
                getLegacyUserId(),
                file
        );

        if (response.getSimilarCaps() != null && !response.getSimilarCaps().isEmpty()) {
            ValidateItemResponse.SimilarItem mostSimilar = response.getSimilarCaps().get(0);
            return mapper.toBottleCapDtoFromSimilarItem(mostSimilar);
        }

        return new BottleCapDto();
    }

    /**
     * Gets all bottle caps as DTOs.
     *
     * @return list of all caps
     */
    public List<BottleCapDto> getAllBottleCapsDto() {
        log.trace("Legacy API: Getting all bottle caps");
        Page<CollectionItemSummary> page = collectionService.searchItemsPaginated(
                getLegacyCollectionKey(),
                null,
                getLegacyUserId(),
                PageRequest.of(0, Integer.MAX_VALUE)
        );
        return mapper.toBottleCapDtoList(page.getContent());
    }

    /**
     * Gets caps by text search with pagination.
     *
     * @param pageNo     page number
     * @param pageSize   page size
     * @param sortBy     sort field
     * @param sortDir    sort direction
     * @param searchText search text
     * @return list of matching caps
     */
    public List<BottleCapDto> findCapByText(int pageNo, int pageSize, String sortBy, String sortDir, String searchText) {
        log.trace("Legacy API: Finding caps by text: {}", searchText);

        Pageable pageable = createPageable(pageNo, pageSize, sortBy, sortDir);
        String query = (searchText == null || searchText.isBlank()) ? null : searchText;

        Page<CollectionItemSummary> page = collectionService.searchItemsPaginated(
                getLegacyCollectionKey(),
                query,
                getLegacyUserId(),
                pageable
        );

        return mapper.toBottleCapDtoList(page.getContent());
    }

    /**
     * Gets all bottle cap links.
     *
     * @return list of cap picture DTOs
     */
    public List<CapPictureDto> getAllBottleCapsLinks() {
        log.trace("Legacy API: Getting all bottle cap links");
        Page<CollectionItemSummary> page = collectionService.searchItemsPaginated(
                getLegacyCollectionKey(),
                null,
                getLegacyUserId(),
                PageRequest.of(0, Integer.MAX_VALUE)
        );
        return mapper.toCapPictureDtoList(page.getContent());
    }

    /**
     * Gets paginated cap picture links.
     *
     * @param pageNo   page number
     * @param pageSize page size
     * @param sortBy   sort field
     * @param sortDir  sort direction
     * @return list of cap picture DTOs
     */
    public List<CapPictureDto> findCapsPaginated(int pageNo, int pageSize, String sortBy, String sortDir) {
        log.trace("Legacy API: Finding caps paginated");

        Pageable pageable = createPageable(pageNo, pageSize, sortBy, sortDir);

        Page<CollectionItemSummary> page = collectionService.searchItemsPaginated(
                getLegacyCollectionKey(),
                null,
                getLegacyUserId(),
                pageable
        );

        return mapper.toCapPictureDtoList(page.getContent());
    }

    /**
     * Gets total count of caps.
     *
     * @return total cap count
     */
    public long getCapAmount() {
        log.trace("Legacy API: Getting cap amount");
        Page<CollectionItemSummary> page = collectionService.searchItemsPaginated(
                getLegacyCollectionKey(),
                null,
                getLegacyUserId(),
                PageRequest.of(0, 1)
        );
        return page.getTotalElements();
    }

    private Pageable createPageable(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(pageNo, pageSize, sort);
    }

    private String getLegacyUserId() {
        return appProperties.getLegacyUserId();
    }

    private String getLegacyCollectionKey() {
        return appProperties.getLegacyCollectionKey();
    }

    private String getLegacyCollectionName() {
        return appProperties.getLegacyCollectionName();
    }
}
