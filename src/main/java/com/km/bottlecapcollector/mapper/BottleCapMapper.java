package com.km.bottlecapcollector.mapper;

import com.km.bottlecapcollector.dto.BottleCapDto;
import com.km.bottlecapcollector.dto.CapPictureDto;
import com.km.bottlecapcollector.model.CapItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface BottleCapMapper {

    BottleCapMapper INSTANCE = Mappers.getMapper(BottleCapMapper.class);

    @Mapping(target = "id", source = "capItem.id")
    @Mapping(target = "url", source = "capItem.image.url")
    CapPictureDto capItemToCapPictureDto(CapItem capItem);


    @Mapping(target = "id", source = "capItem.id")
    @Mapping(target = "url", source = "capItem.image.url")
    @Mapping(target = "name", source = "capItem.name")
    @Mapping(target = "description", source = "capItem.description")
    @Mapping(target = "creationDate", source = "capItem.createDateTime")
    BottleCapDto capItemToBottleCapDto (CapItem capItem);
}
