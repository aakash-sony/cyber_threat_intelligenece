package com.cyberthreat.dashboard.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

	@Value("${cors.allowed-origins:}")
	private String customAllowedOrigins;

	@Bean
	public CorsFilter corsFilter() {
		final var source = new UrlBasedCorsConfigurationSource();
		final var config = new CorsConfiguration();

		config.setAllowCredentials(true);

		final List<String> patterns = new ArrayList<>();
		// Local development origins
		patterns.add("http://localhost:[*]");
		patterns.add("http://127.0.0.1:[*]");
		// Cloud deployment origins (Render subdomains)
		patterns.add("https://*.onrender.com");
		patterns.add("http://*.onrender.com");

		// Custom configured origins
		if (customAllowedOrigins != null && !customAllowedOrigins.isBlank()) {
			final String[] split = customAllowedOrigins.split(",");
			for (final String origin : split) {
				final String trimmed = origin.trim();
				if (!trimmed.isEmpty()) {
					patterns.add(trimmed);
				}
			}
		}

		config.setAllowedOriginPatterns(patterns);
		config.setAllowedHeaders(Arrays.asList(
				"Origin", "Content-Type", "Accept", "Authorization", "X-Requested-With",
				"Access-Control-Request-Method", "Access-Control-Request-Headers"
				));
		config.setAllowedMethods(Arrays.asList(
				"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
				));
		config.setExposedHeaders(Arrays.asList(
				"Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"
				));
		config.setMaxAge(3600L);

		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}
}

