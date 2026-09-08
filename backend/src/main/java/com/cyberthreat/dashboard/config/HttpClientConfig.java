package com.cyberthreat.dashboard.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

	@Bean
	public RestTemplate restTemplate(final RestTemplateBuilder builder) {
		final var httpClient = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_2)
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();

		final var factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(Duration.ofSeconds(15));

		return builder
				.requestFactory(() -> factory)
				.defaultHeader(HttpHeaders.USER_AGENT, "CyberThreatIntelligencePlatform/2.0 (Security Research)")
				.defaultHeader(HttpHeaders.ACCEPT, "*/*")
				.build();
	}
}

