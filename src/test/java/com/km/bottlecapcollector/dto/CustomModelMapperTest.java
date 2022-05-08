package com.km.bottlecapcollector.dto;

import com.km.bottlecapcollector.model.BottleCap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CustomModelMapperTest {

    public static CustomMapper mapper;

    @BeforeClass
    public static void init() throws IOException {
        mapper = new CustomMapper();
    }

    @Test
    public void testBottleCapToBottleCapPictureDto() {
        String location = "testLocation";
        BottleCap cap = new BottleCap();
        cap.setFileLocation(location);
        BottleCapPictureDto capDto = mapper.map(cap, BottleCapPictureDto.class);
        assertEquals(cap.getId(), capDto.getId());
        assertEquals(cap.getFileLocation(), capDto.getUrl());
    }
}
