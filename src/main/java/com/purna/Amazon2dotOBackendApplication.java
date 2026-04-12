package com.purna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.purna.repository")
public class Amazon2dotOBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(Amazon2dotOBackendApplication.class, args);
	}

}
