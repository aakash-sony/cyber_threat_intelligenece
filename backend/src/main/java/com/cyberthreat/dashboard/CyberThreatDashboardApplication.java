package com.cyberthreat.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CyberThreatDashboardApplication {

	public static void main(final String[] args) {
		SpringApplication.run(CyberThreatDashboardApplication.class, args);
	}
}

