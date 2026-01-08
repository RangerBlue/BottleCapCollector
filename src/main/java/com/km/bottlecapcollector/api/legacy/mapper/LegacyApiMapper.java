package com.km.bottlecapcollector.api.legacy.mapper;

import com.km.bottlecapcollector.api.legacy.model.BottleCapDto;
import com.km.bottlecapcollector.api.legacy.model.BottleCapValidationResponseDto;
import com.km.bottlecapcollector.api.legacy.model.CapPictureDto;
import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.ValidateItemResponse;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * MapStruct mapper for converting between new API responses and legacy DTOs.
 */
@Mapper
public interface LegacyApiMapper {

    LegacyApiMapper INSTANCE = Mappers.getMapper(LegacyApiMapper.class);

    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Mapping(target = "url", source = "image.signedUrl")
    @Mapping(target = "creationDate", source = "createdAt", qualifiedByName = "instantToString")
    BottleCapDto toBottleCapDto(CollectionItemResponse response);

    @Named("summaryToBottleCapDto")
    @Mapping(target = "url", source = "signedUrl")
    @Mapping(target = "creationDate", ignore = true)
    BottleCapDto toBottleCapDtoFromSummary(CollectionItemSummary summary);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "url", source = "imageUrl")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "creationDate", ignore = true)
    BottleCapDto toBottleCapDtoFromSimilarItem(ValidateItemResponse.SimilarItem item);

    @Named("summaryToCapPictureDto")
    @Mapping(target = "url", source = "signedUrl")
    CapPictureDto toCapPictureDto(CollectionItemSummary summary);

    @IterableMapping(qualifiedByName = "summaryToBottleCapDto")
    List<BottleCapDto> toBottleCapDtoList(List<CollectionItemSummary> summaries);

    @IterableMapping(qualifiedByName = "summaryToCapPictureDto")
    List<CapPictureDto> toCapPictureDtoList(List<CollectionItemSummary> summaries);

    default BottleCapValidationResponseDto toValidationResponseDto(ValidateItemResponse response) {
        List<String> similarIds = new ArrayList<>();
        List<String> similarUrls = new ArrayList<>();

        if (response.getSimilarCaps() != null) {
            for (ValidateItemResponse.SimilarItem item : response.getSimilarCaps()) {
                similarIds.add(item.getId());
                similarUrls.add(item.getImageUrl());
            }
        }

        return new BottleCapValidationResponseDto(
                response.isHasSimilarItems(),
                similarIds,
                similarUrls,
                new int[0]
        );
    }

    @Named("instantToString")
    default String instantToString(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DATE_FORMATTER.format(instant);
    }
}
