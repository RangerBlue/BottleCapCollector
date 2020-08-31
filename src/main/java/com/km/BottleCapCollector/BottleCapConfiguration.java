package com.km.BottleCapCollector;

import com.km.BottleCapCollector.property.CustomProperties;
import com.km.BottleCapCollector.google.GoogleDriveProperties;
import com.km.BottleCapCollector.util.ImageHistogramUtil;

import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@EnableConfigurationProperties({
        CustomProperties.class, GoogleDriveProperties.class
})
@Configuration
public class BottleCapConfiguration implements CommandLineRunner, ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(BottleCapConfiguration.class);

    @Autowired
    private ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        args.getNonOptionArgs().forEach(logger::info);
        context.getBean(ImageHistogramUtil.class);
    }

    @Override
    public void run(String... args) throws Exception {
        for(String arg : args){
            logger.info(arg);
        }
    }

    @Bean
    public HttpTraceRepository httpTraceRepository() {
        return new InMemoryHttpTraceRepository();
    }

    @Bean
    @ConditionalOnBean(name = "imageHistogramUtil")
    public void conditionalOnBean(){
        logger.info("ImageHistogramUtil is available, name");
    }

    @Bean
    @ConditionalOnBean(ImageHistogramUtil.class)
    public void conditionalOnBeanClass(){
        logger.info("ImageHistogramFactory is available class");
    }

    @Bean
    @ConditionalOnMissingBean(name = "ImageHistogramUtil")
    public void conditionalOnMissingBean(){
        logger.info("ImageHistogramUtil is not available");
    }

    @Bean
    @ConditionalOnProperty(
            value = "server.port",
            havingValue = "8083")
    public void conditionalOnProperty(){
        logger.info("Server port set to 8083");
    }

    @Bean
    @ConditionalOnExpression(
            "${spring.h2.console.enabled.enable:true}")
    public void conditionalOnExpression(){
        logger.info("H2 console access is enabled");
    }

    @Bean
    @ConditionalOnResource(resources = "img/captest.jpg")
    public void conditionalOnResource(){
        logger.info("Test picture captest1 is available");
    }

    @Bean
    @ConditionalOnClass(Imgproc.class)
    public void conditionalOnClass(){
        logger.info("Imgproc class form opencv module is available");
    }

    @Bean
    @ConditionalOnJava(JavaVersion.EIGHT)
    public void conditionalOnJava(){
        logger.info("Java 8 is used");
    }

    @Bean
    @ConditionalOnWebApplication
    public void conditionalOnWebApplication(){
        logger.info("It is web application");
    }

    @Bean
    @ConditionalOnNotWebApplication
    public void conditionalOnNotWebApplication(){
        logger.info("It is not  web application");
    }

    @Bean
    @ConditionalOnCloudPlatform(CloudPlatform.HEROKU)
    public void conditionalOnCloudPlatform(){
        logger.info("Application is run on Heroku");
    }

}
