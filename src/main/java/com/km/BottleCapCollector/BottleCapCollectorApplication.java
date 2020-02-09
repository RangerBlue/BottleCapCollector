package com.km.BottleCapCollector;

import com.km.BottleCapCollector.property.CustomProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties({
		CustomProperties.class
})
public class BottleCapCollectorApplication {
	static {
		System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
	}
	public static void main(String[] args) {
		SpringApplication.run(BottleCapCollectorApplication.class, args);
	}
}
