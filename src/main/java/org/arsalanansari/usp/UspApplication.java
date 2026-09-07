package org.arsalanansari.usp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class UspApplication {

	public static void main(String[] args) {
		SpringApplication.run(UspApplication.class, args);
		log.info("UspApplication started");
	}

}
