package com.km.bottlecapcollector.dto;

import com.km.bottlecapcollector.model.BottleCap;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class CustomMapper extends ModelMapper {
    public CustomMapper() {
        super();
        this.typeMap(BottleCap.class, BottleCapPictureDto.class)
                .addMapping(BottleCap::getFileLocation, BottleCapPictureDto::setUrl);
    }
}
