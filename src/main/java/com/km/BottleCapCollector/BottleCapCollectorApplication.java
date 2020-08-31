package com.km.BottleCapCollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.nio.file.Paths;


@SpringBootApplication
public class BottleCapCollectorApplication {
	static {
		try {
			System.load(String.valueOf(Paths.get(System.getProperty("user.dir"), "lib", "x64", "opencv_java342.dll")));
		} catch (UnsatisfiedLinkError ex) {
			if (!ex.getMessage().contains("already loaded")) {
				throw ex;
			}
		}
	}
	public static void main(String[] args) {
		SpringApplication.run(BottleCapCollectorApplication.class, args);
	}
}
