package com.km.bottlecapcollector.configuration;

import com.km.bottlecapcollector.property.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.system.JavaVersion;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@EnableConfigurationProperties(AppProperties.class)
@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
@Slf4j
public class BottleCapConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    public HttpTraceRepository httpTraceRepository() {
        return new InMemoryHttpTraceRepository();
    }

    @Bean
    @ConditionalOnResource(resources = "img/captest.jpg")
    public void conditionalOnResource() {
        log.info("Test picture captest1 is available");
    }

    @Bean
    @ConditionalOnClass(Imgproc.class)
    public void conditionalOnClass() {
        log.info("Imgproc class form opencv module is available");
    }

    @Bean
    @ConditionalOnJava(JavaVersion.EIGHT)
    public void conditionalOnJava() {
        log.info("Java 8 is used");
    }

    @Bean
    @ConditionalOnWebApplication
    public void conditionalOnWebApplication() {
        log.info("It is web application");
    }


    @Bean
    @ConditionalOnCloudPlatform(CloudPlatform.HEROKU)
    public void conditionalOnCloudPlatform() {
        log.info("Application is run on Heroku");
    }

}
