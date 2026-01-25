package com.km.bottlecapcollector.api.mapper;

import com.km.bottlecapcollector.api.model.response.CollectionItemResponse;
import com.km.bottlecapcollector.api.model.response.CollectionItemSummary;
import com.km.bottlecapcollector.api.model.response.ItemIdentificationResponse;
import com.km.bottlecapcollector.cloud.database.item.entity.ItemEntity;
import com.km.bottlecapcollector.cloud.image.identification.api.ImageIdentification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = CloudEntityMapper.class)
public interface ApiMapper {

    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    CollectionItemResponse toResponse(ItemEntity entity);

    ItemEntity toEntity(CollectionItemResponse response);

    @Mapping(source = "collectionKey", target = "collectionKey")
    @Mapping(source = "image.objectName", target = "objectName")
    CollectionItemSummary toSummary(ItemEntity entity);

    List<CollectionItemResponse> toResponseList(List<ItemEntity> entities);

    List<ItemEntity> toEntityList(List<CollectionItemResponse> responses);

    List<CollectionItemSummary> toSummaryList(List<ItemEntity> entities);

    ItemIdentificationResponse toIdentificationResponse(ImageIdentification identification);
}
