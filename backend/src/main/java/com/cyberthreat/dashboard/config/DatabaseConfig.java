package com.cyberthreat.dashboard.config;

import java.net.URI;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * Production-ready DataSource configuration.
 * Seamlessly supports both local properties (DB_HOST, DB_PORT, etc.) and
 * cloud-native environment variables such as Render's DATABASE_URL (postgres:// or postgresql://).
 */
@Slf4j
@Configuration
public class DatabaseConfig {

	@Value("${spring.datasource.url:}")
	private String defaultUrl;

	@Value("${spring.datasource.username:}")
	private String defaultUsername;

	@Value("${spring.datasource.password:}")
	private String defaultPassword;

	@Value("${spring.datasource.hikari.maximum-pool-size:8}")
	private int maxPoolSize;

	@Value("${spring.datasource.hikari.minimum-idle:2}")
	private int minIdle;

	@Value("${spring.datasource.hikari.connection-timeout:20000}")
	private long connectionTimeout;

	@Value("${spring.datasource.hikari.max-lifetime:600000}")
	private long maxLifetime;

	@Bean
	@Primary
	public DataSource dataSource() {
		String envDatabaseUrl = System.getenv("DATABASE_URL");
		if (envDatabaseUrl == null || envDatabaseUrl.isBlank()) {
			envDatabaseUrl = System.getenv("SPRING_DATASOURCE_URL");
		}

		final var config = new HikariConfig();
		config.setDriverClassName("org.postgresql.Driver");

		if (envDatabaseUrl != null && !envDatabaseUrl.isBlank()) {
			log.info("Configuring DataSource using cloud environment DATABASE_URL.");
			try {
				if (envDatabaseUrl.startsWith("jdbc:postgresql://")) {
					config.setJdbcUrl(envDatabaseUrl);
					if (defaultUsername != null && !defaultUsername.isBlank()) {
						config.setUsername(defaultUsername);
					}
					if (defaultPassword != null && !defaultPassword.isBlank()) {
						config.setPassword(defaultPassword);
					}
				} else {
					// Handle standard cloud postgres URI format: postgres://user:pass@host:port/dbname
					final String normalized = envDatabaseUrl.startsWith("postgres://")
							? envDatabaseUrl.replaceFirst("^postgres://", "http://")
							: envDatabaseUrl.replaceFirst("^postgresql://", "http://");

					final URI uri = URI.create(normalized);
					final String host = uri.getHost();
					final int port = (uri.getPort() == -1) ? 5432 : uri.getPort();
					final String rawPath = uri.getPath();
					final String dbName = (rawPath != null && rawPath.startsWith("/")) ? rawPath.substring(1) : (rawPath != null ? rawPath : "");

					final StringBuilder jdbcUrlBuilder = new StringBuilder();
					jdbcUrlBuilder.append("jdbc:postgresql://").append(host).append(":").append(port).append("/").append(dbName);

					final String query = uri.getQuery();
					if (query != null && !query.isBlank()) {
						jdbcUrlBuilder.append("?").append(query);
						if (!query.contains("sslmode=")) {
							jdbcUrlBuilder.append("&sslmode=require");
						}
					} else {
						jdbcUrlBuilder.append("?sslmode=require");
					}

					config.setJdbcUrl(jdbcUrlBuilder.toString());

					final String userInfo = uri.getUserInfo();
					if (userInfo != null && !userInfo.isBlank()) {
						final String[] credentials = userInfo.split(":", 2);
						config.setUsername(credentials[0]);
						if (credentials.length > 1) {
							config.setPassword(credentials[1]);
						}
					} else {
						if (defaultUsername != null && !defaultUsername.isBlank()) {
							config.setUsername(defaultUsername);
						}
						if (defaultPassword != null && !defaultPassword.isBlank()) {
							config.setPassword(defaultPassword);
						}
					}
				}
			} catch (final Exception e) {
				log.warn("Could not parse cloud DATABASE_URL as URI ({}). Falling back to default URL.", e.getMessage());
				config.setJdbcUrl(defaultUrl);
				config.setUsername(defaultUsername);
				config.setPassword(defaultPassword);
			}
		} else {
			log.info("Configuring DataSource using application properties URL: {}", defaultUrl);
			config.setJdbcUrl(defaultUrl);
			config.setUsername(defaultUsername);
			config.setPassword(defaultPassword);
		}

		config.setMaximumPoolSize(maxPoolSize);
		config.setMinimumIdle(minIdle);
		config.setConnectionTimeout(connectionTimeout);
		config.setMaxLifetime(maxLifetime);
		config.setIdleTimeout(30000);
		config.setPoolName("CyberThreatHikariPool");

		return new HikariDataSource(config);
	}
}
