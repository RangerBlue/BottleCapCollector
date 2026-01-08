package com.km.bottlecapcollector.api.mapper;

import com.km.bottlecapcollector.api.model.response.ImageAnalysisMetadataResponse;
import com.km.bottlecapcollector.api.model.response.ImageResponse;
import com.km.bottlecapcollector.cloud.database.item.entity.ImageAnalysisMetadataEntity;
import com.km.bottlecapcollector.cloud.database.item.entity.StorageImageEntity;
import com.km.bottlecapcollector.cloud.image.analysis.api.ImageAnalysisMetadata;
import com.km.bottlecapcollector.cloud.storage.api.StorageImage;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CloudEntityMapper {

    CloudEntityMapper INSTANCE = Mappers.getMapper(CloudEntityMapper.class);

    ImageResponse toImageResponse(StorageImage image);

    ImageResponse toImageResponse(StorageImageEntity entity);

    StorageImage toStorageImage(ImageResponse response);

    // ImageAnalysisMetadata mappings
    ImageAnalysisMetadataResponse toMetadataResponse(ImageAnalysisMetadata metadata);

    ImageAnalysisMetadataResponse toMetadataResponse(ImageAnalysisMetadataEntity entity);

    ImageAnalysisMetadata toImageAnalysisMetadata(ImageAnalysisMetadataResponse response);

    // Nested type mappings for ImageAnalysisMetadata
    ImageAnalysisMetadataResponse.VisionLabelDto toVisionLabelDto(ImageAnalysisMetadata.ImageLabel label);

    ImageAnalysisMetadata.ImageLabel toImageLabel(ImageAnalysisMetadataResponse.VisionLabelDto dto);

    ImageAnalysisMetadataResponse.VisionColorDto toVisionColorDto(ImageAnalysisMetadata.ImageColor color);

    ImageAnalysisMetadata.ImageColor toImageColor(ImageAnalysisMetadataResponse.VisionColorDto dto);

    ImageAnalysisMetadataResponse.VisionTextDto toVisionTextDto(ImageAnalysisMetadata.ImageText text);

    ImageAnalysisMetadata.ImageText toImageText(ImageAnalysisMetadataResponse.VisionTextDto dto);

    ImageAnalysisMetadataResponse.VisionLogoDto toVisionLogoDto(ImageAnalysisMetadata.ImageLogo logo);

    ImageAnalysisMetadata.ImageLogo toImageLogo(ImageAnalysisMetadataResponse.VisionLogoDto dto);

    // Nested type mappings for Entity types
    ImageAnalysisMetadataResponse.VisionLabelDto toVisionLabelDto(ImageAnalysisMetadataEntity.FirestoreImageLabel label);

    ImageAnalysisMetadataResponse.VisionColorDto toVisionColorDto(ImageAnalysisMetadataEntity.FirestoreImageColor color);

    ImageAnalysisMetadataResponse.VisionTextDto toVisionTextDto(ImageAnalysisMetadataEntity.FirestoreImageText text);

    ImageAnalysisMetadataResponse.VisionLogoDto toVisionLogoDto(ImageAnalysisMetadataEntity.FirestoreImageLogo logo);
}
