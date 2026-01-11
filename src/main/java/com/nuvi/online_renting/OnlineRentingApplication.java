package com.nuvi.online_renting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class OnlineRentingApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineRentingApplication.class, args);
	}
}
