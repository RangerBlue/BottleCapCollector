package com.km.BottleCapCollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.nio.file.Paths;


@SpringBootApplication
public class BottleCapCollectorApplication {
	static{
		nu.pattern.OpenCV.loadShared();
	}
	public static void main(String[] args) {
		SpringApplication.run(BottleCapCollectorApplication.class, args);
	}
}
