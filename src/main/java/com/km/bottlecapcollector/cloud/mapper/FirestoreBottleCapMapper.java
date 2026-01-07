package com.km.bottlecapcollector.cloud.mapper;

import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.ImageAnalysisMetadataResponse;
import com.km.bottlecapcollector.api.model.response.ImageResponse;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.image.analysis.api.ImageAnalysisMetadata;
import com.km.bottlecapcollector.cloud.storage.api.StorageImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MapStruct mapper for converting between Firestore documents and DTOs.
 */
@Mapper
public interface FirestoreBottleCapMapper {

    FirestoreBottleCapMapper INSTANCE = Mappers.getMapper(FirestoreBottleCapMapper.class);

    CollectionItemResponse toDto(ItemEntity document);

    ItemEntity toDocument(CollectionItemResponse dto);

    ImageResponse toImageDto(StorageImage image);

    StorageImage toImageDocument(ImageResponse dto);

    ImageAnalysisMetadataResponse toVisionMetadataDto(ImageAnalysisMetadata metadata);

    ImageAnalysisMetadata toVisionMetadataDocument(ImageAnalysisMetadataResponse dto);

    ImageAnalysisMetadataResponse.VisionLabelDto toVisionLabelDto(ImageAnalysisMetadata.ImageLabel imageLabel);

    ImageAnalysisMetadata.ImageLabel toVisionLabelDocument(ImageAnalysisMetadataResponse.VisionLabelDto dto);

    ImageAnalysisMetadataResponse.VisionColorDto toVisionColorDto(ImageAnalysisMetadata.ImageColor color);

    ImageAnalysisMetadata.ImageColor toVisionColorDocument(ImageAnalysisMetadataResponse.VisionColorDto dto);

    ImageAnalysisMetadataResponse.VisionTextDto toVisionTextDto(ImageAnalysisMetadata.ImageText text);

    ImageAnalysisMetadata.ImageText toVisionTextDocument(ImageAnalysisMetadataResponse.VisionTextDto dto);

    ImageAnalysisMetadataResponse.VisionLogoDto toVisionLogoDto(ImageAnalysisMetadata.ImageLogo logo);

    ImageAnalysisMetadata.ImageLogo toVisionLogoDocument(ImageAnalysisMetadataResponse.VisionLogoDto dto);

    List<CollectionItemResponse> toDtoList(List<ItemEntity> documents);

    List<ItemEntity> toDocumentList(List<CollectionItemResponse> dtos);

    @Mapping(source = "collectionKey", target = "collectionKey")
    @Mapping(source = "image.objectName", target = "objectName")
    CollectionItemSummary toSummary(ItemEntity document);

    List<CollectionItemSummary> toSummaryList(List<ItemEntity> documents);
}
