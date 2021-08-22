package com.km.bottlecapcollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class BottleCapCollectorApplication {
	static{
		nu.pattern.OpenCV.loadShared();
	}
	public static void main(String[] args) {
		SpringApplication.run(BottleCapCollectorApplication.class, args);
	}
}
