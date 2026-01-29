package com.km.bottlecapcollector.configuration;

import com.km.bottlecapcollector.property.AppProperties;
import com.km.bottlecapcollector.color.HSBColorService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableConfigurationProperties(AppProperties.class)
@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@Slf4j
@AllArgsConstructor
public class BottleCapConfiguration {
    private final AppProperties appProperties;

    @PostConstruct
    public void setupUtilityClasses(){
        float margin = appProperties.getSimilaritySearchRange();
        log.info("Setting HSBColorService with margin value {} ", margin);
        HSBColorService.setMargin(margin);
    }
}
