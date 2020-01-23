package com.km.BottleCapCollector;

import com.km.BottleCapCollector.property.FileStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties({
		FileStorageProperties.class
})
public class BottleCapCollectorApplication {
	public static void main(String[] args) {
		SpringApplication.run(BottleCapCollectorApplication.class, args);
	}
}
