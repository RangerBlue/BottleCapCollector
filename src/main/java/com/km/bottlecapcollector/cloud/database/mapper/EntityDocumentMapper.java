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


    HSBColorEntity toFirestore(HSBColor source);

    HSBColor toDomain(HSBColorEntity source);

    StorageImageEntity toFirestore(StorageImage source);

    StorageImage toDomain(StorageImageEntity source);

    EmbeddingEntity toFirestore(Embedding source);

    Embedding toDomain(EmbeddingEntity source);

    ImageAnalysisMetadataEntity toFirestore(ImageAnalysisMetadata source);

    ImageAnalysisMetadata toDomain(ImageAnalysisMetadataEntity source);

    ImageAnalysisMetadataEntity.FirestoreImageLabel toFirestore(ImageAnalysisMetadata.ImageLabel source);

    ImageAnalysisMetadata.ImageLabel toDomain(ImageAnalysisMetadataEntity.FirestoreImageLabel source);

    ImageAnalysisMetadataEntity.FirestoreImageColor toFirestore(ImageAnalysisMetadata.ImageColor source);

    ImageAnalysisMetadata.ImageColor toDomain(ImageAnalysisMetadataEntity.FirestoreImageColor source);

    ImageAnalysisMetadataEntity.FirestoreImageText toFirestore(ImageAnalysisMetadata.ImageText source);

    ImageAnalysisMetadata.ImageText toDomain(ImageAnalysisMetadataEntity.FirestoreImageText source);

    ImageAnalysisMetadataEntity.FirestoreImageLogo toFirestore(ImageAnalysisMetadata.ImageLogo source);

    ImageAnalysisMetadata.ImageLogo toDomain(ImageAnalysisMetadataEntity.FirestoreImageLogo source);

    List<ImageAnalysisMetadataEntity.FirestoreImageLabel> toFirestoreLabels(List<ImageAnalysisMetadata.ImageLabel> source);

    List<ImageAnalysisMetadata.ImageLabel> toDomainLabels(List<ImageAnalysisMetadataEntity.FirestoreImageLabel> source);

    List<ImageAnalysisMetadataEntity.FirestoreImageColor> toFirestoreColors(List<ImageAnalysisMetadata.ImageColor> source);

    List<ImageAnalysisMetadata.ImageColor> toDomainColors(List<ImageAnalysisMetadataEntity.FirestoreImageColor> source);
}
