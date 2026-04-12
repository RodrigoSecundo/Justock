package com.justeam.justock_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JustockApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(JustockApiApplication.class, args);
	}

}
