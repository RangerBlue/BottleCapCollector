package com.km.bottlecapcollector;

import com.km.bottlecapcollector.property.CustomProperties;
import com.km.bottlecapcollector.google.GoogleDriveProperties;
import com.km.bottlecapcollector.util.ImageHistogramUtil;

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

@EnableConfigurationProperties({
        CustomProperties.class, GoogleDriveProperties.class
})
@Configuration
@EnableCaching
@EnableAsync
@Slf4j
public class BottleCapConfiguration implements CommandLineRunner, ApplicationRunner {

    @Autowired
    private ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        args.getNonOptionArgs().forEach(log::info);
        context.getBean(ImageHistogramUtil.class);
    }

    @Override
    public void run(String... args) throws Exception {
        for(String arg : args){
            log.info(arg);
        }
    }

    @Bean
    public HttpTraceRepository httpTraceRepository() {
        return new InMemoryHttpTraceRepository();
    }

    @Bean
    @ConditionalOnBean(name = "imageHistogramUtil")
    public void conditionalOnBean(){
        log.info("ImageHistogramUtil is available, name");
    }

    @Bean
    @ConditionalOnBean(ImageHistogramUtil.class)
    public void conditionalOnBeanClass(){
        log.info("ImageHistogramFactory is available class");
    }

    @Bean
    @ConditionalOnMissingBean(name = "ImageHistogramUtil")
    public void conditionalOnMissingBean(){
        log.info("ImageHistogramUtil is not available");
    }

    @Bean
    @ConditionalOnProperty(
            value = "server.port",
            havingValue = "8083")
    public void conditionalOnProperty(){
        log.info("Server port set to 8083");
    }

    @Bean
    @ConditionalOnExpression(
            "${spring.h2.console.enabled.enable:true}")
    public void conditionalOnExpression(){
        log.info("H2 console access is enabled");
    }

    @Bean
    @ConditionalOnResource(resources = "img/captest.jpg")
    public void conditionalOnResource(){
        log.info("Test picture captest1 is available");
    }

    @Bean
    @ConditionalOnClass(Imgproc.class)
    public void conditionalOnClass(){
        log.info("Imgproc class form opencv module is available");
    }

    @Bean
    @ConditionalOnJava(JavaVersion.EIGHT)
    public void conditionalOnJava(){
        log.info("Java 8 is used");
    }

    @Bean
    @ConditionalOnWebApplication
    public void conditionalOnWebApplication(){
        log.info("It is web application");
    }

    @Bean
    @ConditionalOnNotWebApplication
    public void conditionalOnNotWebApplication(){
        log.info("It is not  web application");
    }

    @Bean
    @ConditionalOnCloudPlatform(CloudPlatform.HEROKU)
    public void conditionalOnCloudPlatform(){
        log.info("Application is run on Heroku");
    }

}
