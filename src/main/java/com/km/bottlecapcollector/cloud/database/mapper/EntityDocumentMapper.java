package com.km.bottlecapcollector.cloud.database.mapper;

import com.km.bottlecapcollector.cloud.database.item.entity.EmbeddingEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.HSBColorEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.ImageAnalysisMetadataEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.StorageImageEntity;
import com.km.bottlecapcollector.cloud.image.analysis.api.ImageAnalysisMetadata;
import com.km.bottlecapcollector.cloud.image.ml.api.Embedding;
import com.km.bottlecapcollector.cloud.storage.api.StorageImage;
import com.km.bottlecapcollector.color.HSBColor;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface EntityDocumentMapper {

    EntityDocumentMapper INSTANCE = Mappers.getMapper(EntityDocumentMapper.class);


    HSBColorEntity toEntity(HSBColor source);

    HSBColor toDomain(HSBColorEntity source);

    StorageImageEntity toEntity(StorageImage source);

    StorageImage toDomain(StorageImageEntity source);

    EmbeddingEntity toEntity(Embedding source);

    Embedding toDomain(EmbeddingEntity source);

    ImageAnalysisMetadataEntity toEntity(ImageAnalysisMetadata source);

    ImageAnalysisMetadata toDomain(ImageAnalysisMetadataEntity source);

    ImageAnalysisMetadataEntity.FirestoreImageLabel toEntity(ImageAnalysisMetadata.ImageLabel source);

    ImageAnalysisMetadata.ImageLabel toDomain(ImageAnalysisMetadataEntity.FirestoreImageLabel source);

    ImageAnalysisMetadataEntity.FirestoreImageColor toEntity(ImageAnalysisMetadata.ImageColor source);

    ImageAnalysisMetadata.ImageColor toDomain(ImageAnalysisMetadataEntity.FirestoreImageColor source);

    ImageAnalysisMetadataEntity.FirestoreImageText toEntity(ImageAnalysisMetadata.ImageText source);

    ImageAnalysisMetadata.ImageText toDomain(ImageAnalysisMetadataEntity.FirestoreImageText source);

    ImageAnalysisMetadataEntity.FirestoreImageLogo toEntity(ImageAnalysisMetadata.ImageLogo source);

    ImageAnalysisMetadata.ImageLogo toDomain(ImageAnalysisMetadataEntity.FirestoreImageLogo source);

    List<ImageAnalysisMetadataEntity.FirestoreImageLabel> toEntityLabels(List<ImageAnalysisMetadata.ImageLabel> source);

    List<ImageAnalysisMetadata.ImageLabel> toDomainLabels(List<ImageAnalysisMetadataEntity.FirestoreImageLabel> source);

    List<ImageAnalysisMetadataEntity.FirestoreImageColor> toEntityColors(List<ImageAnalysisMetadata.ImageColor> source);

    List<ImageAnalysisMetadata.ImageColor> toDomainColors(List<ImageAnalysisMetadataEntity.FirestoreImageColor> source);
}
