package com.km.bottlecapcollector.firestore.mapper;

import com.km.bottlecapcollector.firestore.document.CollectionItem;
import com.km.bottlecapcollector.firestore.document.FirestoreEmbedding;
import com.km.bottlecapcollector.firestore.document.FirestoreHSBColor;
import com.km.bottlecapcollector.firestore.document.FirestoreImage;
import com.km.bottlecapcollector.firestore.document.FirestoreVisionMetadata;
import com.km.bottlecapcollector.firestore.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MapStruct mapper for converting between Firestore documents and DTOs.
 */
@Mapper
public interface FirestoreBottleCapMapper {

    FirestoreBottleCapMapper INSTANCE = Mappers.getMapper(FirestoreBottleCapMapper.class);

    CollectionItemResponse toDto(CollectionItem document);

    CollectionItem toDocument(CollectionItemResponse dto);

    FirestoreImageDto toImageDto(FirestoreImage image);

    FirestoreImage toImageDocument(FirestoreImageDto dto);

    FirestoreHSBColorDto toHSBColorDto(FirestoreHSBColor hsbColor);

    FirestoreHSBColor toHSBColorDocument(FirestoreHSBColorDto dto);

    FirestoreEmbeddingDto toEmbeddingDto(FirestoreEmbedding embedding);

    FirestoreEmbedding toEmbeddingDocument(FirestoreEmbeddingDto dto);

    FirestoreVisionMetadataDto toVisionMetadataDto(FirestoreVisionMetadata metadata);

    FirestoreVisionMetadata toVisionMetadataDocument(FirestoreVisionMetadataDto dto);

    FirestoreVisionMetadataDto.VisionLabelDto toVisionLabelDto(FirestoreVisionMetadata.VisionLabel label);

    FirestoreVisionMetadata.VisionLabel toVisionLabelDocument(FirestoreVisionMetadataDto.VisionLabelDto dto);

    FirestoreVisionMetadataDto.VisionColorDto toVisionColorDto(FirestoreVisionMetadata.VisionColor color);

    FirestoreVisionMetadata.VisionColor toVisionColorDocument(FirestoreVisionMetadataDto.VisionColorDto dto);

    FirestoreVisionMetadataDto.VisionTextDto toVisionTextDto(FirestoreVisionMetadata.VisionText text);

    FirestoreVisionMetadata.VisionText toVisionTextDocument(FirestoreVisionMetadataDto.VisionTextDto dto);

    FirestoreVisionMetadataDto.VisionLogoDto toVisionLogoDto(FirestoreVisionMetadata.VisionLogo logo);

    FirestoreVisionMetadata.VisionLogo toVisionLogoDocument(FirestoreVisionMetadataDto.VisionLogoDto dto);


    List<CollectionItemResponse> toDtoList(List<CollectionItem> documents);

    List<CollectionItem> toDocumentList(List<CollectionItemResponse> dtos);
}
