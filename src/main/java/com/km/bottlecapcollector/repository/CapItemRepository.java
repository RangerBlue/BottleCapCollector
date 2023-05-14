package com.km.bottlecapcollector.repository;

import com.km.bottlecapcollector.model.CapItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CapItemRepository extends CollectionItemRepository<CapItem> {
    Page<CapItem> findByNameContainingOrDescriptionContainingAllIgnoreCase(String name, String desc, Pageable pageable);

    @Query(value = "SELECT cap FROM CollectionItem cap " +
            "JOIN FETCH cap.image ai " +
            "JOIN FETCH ai.signature " +
            "JOIN FETCH ai.provider " +
            "WHERE (ai.hue > :hueMoreThan AND ai.hue <= :huePivotLessThan " +
            "OR ai.hue > :huePivotMoreThan AND ai.hue <= :hueLessThan) " +
            "AND (ai.saturation > :saturationMoreThan AND ai.saturation <= :saturationPivotLessThan " +
            "OR ai.saturation > :saturationPivotMoreThan AND ai.saturation <= :saturationLessThan) " +
            "AND (ai.brightness > :brightnessMoreThan AND ai.brightness <= :brightnessPivotLessThan " +
            "OR ai.brightness > :brightnessPivotMoreThan AND ai.brightness <= :brightnessLessThan)" +
            "AND TYPE(cap) = CapItem")
    List<CapItem> findByColorsValues(
            @Param("hueMoreThan") float hueMoreThan,
            @Param("huePivotLessThan") float huePivotLessThan,
            @Param("huePivotMoreThan") float huePivotMoreThan,
            @Param("hueLessThan") float hueLessThan,
            @Param("saturationMoreThan") float saturationMoreThan,
            @Param("saturationPivotLessThan") float saturationPivotLessThan,
            @Param("saturationPivotMoreThan") float saturationPivotMoreThan,
            @Param("saturationLessThan") float saturationLessThan,
            @Param("brightnessMoreThan") float brightnessMoreThan,
            @Param("brightnessPivotLessThan") float brightnessPivotLessThan,
            @Param("brightnessPivotMoreThan") float brightnessPivotMoreThan,
            @Param("brightnessLessThan") float brightnessLessThan,
            Pageable pageable);

    @Query(value = "SELECT COUNT(ci) FROM CollectionItem ci WHERE TYPE(ci) = CapItem ")
    long countAllCapItems();

}
