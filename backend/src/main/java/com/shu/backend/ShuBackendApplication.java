package com.shu.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ShuBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShuBackendApplication.class, args);
	}

}
