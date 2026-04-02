package com.nuvi.online_renting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableScheduling
public class OnlineRentingApplication {

	public static void main(String[] args) {
		// Force the JVM to UTC before Spring initialises any beans.
		// Without this, LocalDateTime.now() and @CreatedDate pick up the server's
		// local timezone, which breaks timestamp consistency across environments.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(OnlineRentingApplication.class, args);
	}
}
