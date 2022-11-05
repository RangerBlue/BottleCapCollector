package com.km.bottlecapcollector.mapper;

import com.km.bottlecapcollector.dto.ComparisonRangeDto;
import com.km.bottlecapcollector.model.ComparisonRange;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ComparisonRangeMapper {
    ComparisonRangeMapper INSTANCE = Mappers.getMapper(ComparisonRangeMapper.class);

    ComparisonRangeDto comparisonRangeToComparisonRangeDto(ComparisonRange comparisonRange);
}
