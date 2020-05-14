package com.km.BottleCapCollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class BottleCapCollectorApplication {
	static {
		System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
	}
	public static void main(String[] args) {
		SpringApplication.run(BottleCapCollectorApplication.class, args);
	}
}
