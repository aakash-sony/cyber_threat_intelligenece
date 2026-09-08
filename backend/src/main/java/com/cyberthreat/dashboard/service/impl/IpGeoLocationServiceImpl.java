package com.cyberthreat.dashboard.service.impl;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.cyberthreat.dashboard.constant.ThreatApiConstants;
import com.cyberthreat.dashboard.dto.response.GeoLocationDto;
import com.cyberthreat.dashboard.entity.IpGeolocationEntity;
import com.cyberthreat.dashboard.enums.IndicatorType;
import com.cyberthreat.dashboard.repository.IpGeolocationRepository;
import com.cyberthreat.dashboard.service.GeoLocationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Production implementation of GeoLocationService using external public IP geolocation APIs
 * and PostgreSQL database caching.
 *
 * Distinguishes infrastructure location from physical attacker location.
 * Does not fabricate country data; returns 'Unknown' if unresolvable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpGeoLocationServiceImpl implements GeoLocationService {

	private final IpGeolocationRepository ipGeolocationRepository;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${geo.api.enabled:true}")
	private boolean geoApiEnabled = true;

	@Value("${geo.api.base-url:https://ipapi.co}")
	private String geoApiBaseUrl = ThreatApiConstants.IPAPI_BASE_URL;

	private static final Pattern IPV4_PATTERN = Pattern.compile(
			"^(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
			);

	private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
			"^(?:10\\.|127\\.|172\\.(?:1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.|0\\.|169\\.254\\.).*"
			);

	@Override
	public GeoLocationDto resolveIp(final String ip) {
		if (ip == null || ip.isBlank())
			return GeoLocationDto.unknown(ip, "Blank/Null IP");

		final var cleanIp = ip.trim();
		if (!IPV4_PATTERN.matcher(cleanIp).matches())
			return GeoLocationDto.unknown(cleanIp, "Invalid IPv4 format");

		if (PRIVATE_IP_PATTERN.matcher(cleanIp).matches())
			return GeoLocationDto.unknown(cleanIp, "Private/Bogon IP");

		// 1. Check database cache first to prevent redundant external API calls
		try {
			final var cached = ipGeolocationRepository.findByIp(cleanIp);
			if (cached.isPresent()) {
				log.debug("Geolocation cache hit for IP: {}", cleanIp);
				return mapEntityToDto(cached.get());
			}
		} catch (final Exception e) {
			log.warn("Database error reading geolocation cache for IP {}: {}", cleanIp, e.getMessage());
		}

		if (!geoApiEnabled)
			return GeoLocationDto.unknown(cleanIp, "Geo API Disabled");

		// 2. Query external public IP geolocation provider: primary ip-api.com, secondary ipapi.co
		try {
			// Try primary: ip-api.com
			try {
				final var ipApiUrl = "http://ip-api.com/json/" + cleanIp;
				log.debug("Querying primary IP geolocation API: {}", ipApiUrl);
				final ResponseEntity<String> response = restTemplate.getForEntity(ipApiUrl, String.class);
				if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isBlank()) {
					final var root = objectMapper.readTree(response.getBody());
					if ("success".equalsIgnoreCase(root.path("status").asText())) {
						final var countryName = root.path("country").asText("Unknown").trim();
						final var countryCode = root.path("countryCode").asText("").trim();
						final var region = root.path("regionName").asText("").trim();
						final var city = root.path("city").asText("").trim();
						final var latitude = root.has("lat") && !root.path("lat").isNull() ? root.path("lat").asDouble() : null;
						final var longitude = root.has("lon") && !root.path("lon").isNull() ? root.path("lon").asDouble() : null;
						final var asn = root.path("as").asText("").trim();
						final var org = root.path("org").asText("").trim();

						final var entity = IpGeolocationEntity.builder()
								.ip(cleanIp)
								.countryCode(countryCode.isBlank() ? null : countryCode)
								.countryName(countryName.isBlank() ? "Unknown" : countryName)
								.region(region.isBlank() ? null : region)
								.city(city.isBlank() ? null : city)
								.latitude(latitude)
								.longitude(longitude)
								.asn(asn.isBlank() ? null : asn)
								.organization(org.isBlank() ? null : org)
								.source("IP Geolocation (http://ip-api.com)")
								.lastUpdated(LocalDateTime.now())
								.build();

						try {
							final var saved = ipGeolocationRepository.save(entity);
							return mapEntityToDto(saved);
						} catch (final Exception e) {
							return mapEntityToDto(entity);
						}
					}
				}
			} catch (final Exception e) {
				log.debug("Primary ip-api.com lookup failed for {}: {}", cleanIp, e.getMessage());
			}

			// Fallback: ipapi.co
			final var baseUrl = geoApiBaseUrl != null && !geoApiBaseUrl.isBlank()
					? geoApiBaseUrl.replaceAll("/+$", "")
							: ThreatApiConstants.IPAPI_BASE_URL;
			final var requestUrl = baseUrl + "/" + cleanIp + "/json/";

			final var headers = new HttpHeaders();
			headers.set(HttpHeaders.USER_AGENT, "CyberThreatIntelligencePlatform/2.0");
			headers.set(HttpHeaders.ACCEPT, "application/json");
			final var requestEntity = new HttpEntity<Void>(headers);

			final ResponseEntity<String> response = restTemplate.exchange(
					requestUrl,
					HttpMethod.GET,
					requestEntity,
					String.class
					);

			if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isBlank()) {
				final var root = objectMapper.readTree(response.getBody());
				if (!root.path("error").asBoolean(false)) {
					final var countryName = root.path("country_name").asText("Unknown").trim();
					final var countryCode = root.path("country_code").asText("").trim();
					final var region = root.path("region").asText("").trim();
					final var city = root.path("city").asText("").trim();
					final var latitude = root.has("latitude") && !root.path("latitude").isNull() ? root.path("latitude").asDouble() : null;
					final var longitude = root.has("longitude") && !root.path("longitude").isNull() ? root.path("longitude").asDouble() : null;
					final var asn = root.path("asn").asText("").trim();
					final var org = root.path("org").asText("").trim();

					final var entity = IpGeolocationEntity.builder()
							.ip(cleanIp)
							.countryCode(countryCode.isBlank() ? null : countryCode)
							.countryName(countryName.isBlank() ? "Unknown" : countryName)
							.region(region.isBlank() ? null : region)
							.city(city.isBlank() ? null : city)
							.latitude(latitude)
							.longitude(longitude)
							.asn(asn.isBlank() ? null : asn)
							.organization(org.isBlank() ? null : org)
							.source("IP Geolocation (" + baseUrl + ")")
							.lastUpdated(LocalDateTime.now())
							.build();

					try {
						final var saved = ipGeolocationRepository.save(entity);
						return mapEntityToDto(saved);
					} catch (final Exception e) {
						return mapEntityToDto(entity);
					}
				} else {
					final var reason = root.path("reason").asText("Provider Error");
					return GeoLocationDto.unknown(cleanIp, "Provider Error: " + reason);
				}
			}

			return GeoLocationDto.unknown(cleanIp, "Geolocation Unresolved");

		} catch (final Exception e) {
			log.warn("Geolocation lookup failed for IP {}: {}", cleanIp, e.getMessage());
			return GeoLocationDto.unknown(cleanIp, "Exception: " + e.getMessage());
		}
	}

	@Override
	public GeoLocationDto resolveIndicator(final String indicator, final IndicatorType indicatorType) {
		if (indicator == null || indicator.isBlank())
			return GeoLocationDto.unknown(indicator, "Empty Indicator");

		final var clean = indicator.trim();

		// 1. Direct IPv4 indicator
		if (indicatorType == IndicatorType.IP || IPV4_PATTERN.matcher(clean).matches())
			return resolveIp(clean);

		// 2. Hash indicators have no associated IP
		if (indicatorType == IndicatorType.HASH)
			return GeoLocationDto.unknown(clean, "Hash Indicator (No IP)");

		// 3. URL or Domain indicator -> Extract hostname -> DNS resolution -> IP
		final var host = extractHostOrIp(clean, indicatorType);
		if (host == null || host.isBlank())
			return GeoLocationDto.unknown(clean, "Unresolvable Host");

		// If the extracted host is already an IP
		if (IPV4_PATTERN.matcher(host).matches())
			return resolveIp(host);

		// Perform DNS resolution
		try {
			final var address = InetAddress.getByName(host);
			final var resolvedIp = address.getHostAddress();
			log.debug("DNS resolved domain '{}' to IP: {}", host, resolvedIp);
			return resolveIp(resolvedIp);
		} catch (final UnknownHostException e) {
			log.debug("DNS lookup failed for host '{}': host unknown / unreachable", host);
			return GeoLocationDto.unknown(clean, "DNS Resolution Failed");
		} catch (final Exception e) {
			log.warn("DNS resolution error for host '{}': {}", host, e.getMessage());
			return GeoLocationDto.unknown(clean, "DNS Error: " + e.getMessage());
		}
	}

	@Override
	public Map<String, GeoLocationDto> resolveIndicatorsBatch(final Collection<String> indicators) {
		if (indicators == null || indicators.isEmpty())
			return Collections.emptyMap();

		final Map<String, GeoLocationDto> resultMap = new HashMap<>();
		final Set<String> uniqueIndicators = new LinkedHashSet<>(indicators);
		final Map<String, String> indicatorToIpMap = new HashMap<>();

		// Extract or DNS-resolve IPs for all indicators first
		for (final String ind : uniqueIndicators) {
			if (ind == null || ind.isBlank()) continue;
			final var clean = ind.trim();

			if (IPV4_PATTERN.matcher(clean).matches())
				indicatorToIpMap.put(clean, clean);
			else {
				final var host = extractHostOrIp(clean, null);
				if (host != null && IPV4_PATTERN.matcher(host).matches())
					indicatorToIpMap.put(clean, host);
				else if (host != null && !host.isBlank())
					resultMap.put(clean, GeoLocationDto.unknown(clean, "Domain Indicator"));
			}
		}

		// Batch query DB cache for all unique IPs
		final Set<String> uniqueIps = new HashSet<>(indicatorToIpMap.values());
		uniqueIps.removeIf(ip -> !IPV4_PATTERN.matcher(ip).matches() || PRIVATE_IP_PATTERN.matcher(ip).matches());

		final Map<String, GeoLocationDto> ipCache = new HashMap<>();
		if (!uniqueIps.isEmpty())
			try {
				final var cachedEntities = ipGeolocationRepository.findByIpIn(uniqueIps);
				for (final IpGeolocationEntity entity : cachedEntities)
					ipCache.put(entity.getIp(), mapEntityToDto(entity));
			} catch (final Exception e) {
				log.warn("Batch cache lookup failed: {}", e.getMessage());
			}

		// Resolve remaining uncached IPs in batch using http://ip-api.com/batch (up to 100 IPs per call)
		final List<String> uncachedIps = new ArrayList<>();
		for (final String ip : uniqueIps) {
			if (!ipCache.containsKey(ip)) {
				uncachedIps.add(ip);
			}
		}

		if (!uncachedIps.isEmpty() && geoApiEnabled) {
			final int batchSize = 100;
			final List<IpGeolocationEntity> newEntities = new ArrayList<>();

			for (int i = 0; i < uncachedIps.size(); i += batchSize) {
				final List<String> batch = uncachedIps.subList(i, Math.min(i + batchSize, uncachedIps.size()));
				try {
					final var headers = new HttpHeaders();
					headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
					final var requestEntity = new HttpEntity<>(batch, headers);
					final ResponseEntity<String> response = restTemplate.exchange("http://ip-api.com/batch", HttpMethod.POST, requestEntity, String.class);

					if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isBlank()) {
						final JsonNode arrayNode = objectMapper.readTree(response.getBody());
						if (arrayNode.isArray()) {
							for (final JsonNode item : arrayNode) {
								final String ip = item.path("query").asText("").trim();
								if (ip.isBlank()) continue;

								if ("success".equalsIgnoreCase(item.path("status").asText())) {
									final String countryName = item.path("country").asText("Unknown").trim();
									final String countryCode = item.path("countryCode").asText("").trim();
									final String region = item.path("regionName").asText("").trim();
									final String city = item.path("city").asText("").trim();
									final Double lat = item.has("lat") && !item.path("lat").isNull() ? item.path("lat").asDouble() : null;
									final Double lon = item.has("lon") && !item.path("lon").isNull() ? item.path("lon").asDouble() : null;
									final String asn = item.path("as").asText("").trim();
									final String org = item.path("org").asText("").trim();

									final var entity = IpGeolocationEntity.builder()
											.ip(ip)
											.countryCode(countryCode.isBlank() ? null : countryCode)
											.countryName(countryName.isBlank() ? "Unknown" : countryName)
											.region(region.isBlank() ? null : region)
											.city(city.isBlank() ? null : city)
											.latitude(lat)
											.longitude(lon)
											.asn(asn.isBlank() ? null : asn)
											.organization(org.isBlank() ? null : org)
											.source("IP Geolocation (http://ip-api.com)")
											.lastUpdated(LocalDateTime.now())
											.build();
									newEntities.add(entity);
									ipCache.put(ip, mapEntityToDto(entity));
								} else {
									final var entity = IpGeolocationEntity.builder()
											.ip(ip)
											.countryName("Unknown")
											.source("IP Geolocation (Unresolved)")
											.lastUpdated(LocalDateTime.now())
											.build();
									newEntities.add(entity);
									ipCache.put(ip, mapEntityToDto(entity));
								}
							}
						}
					}
				} catch (final Exception e) {
					log.warn("Batch IP geolocation lookup failed: {}", e.getMessage());
					for (final String ip : batch) {
						if (!ipCache.containsKey(ip)) {
							ipCache.put(ip, GeoLocationDto.unknown(ip, "Rate Limited / Unresolved"));
						}
					}
				}
			}

			if (!newEntities.isEmpty()) {
				try {
					ipGeolocationRepository.saveAll(newEntities);
				} catch (final Exception e) {
					log.warn("Failed to persist batch geolocation cache: {}", e.getMessage());
				}
			}
		}

		// Map back to indicators
		for (final String ind : uniqueIndicators) {
			if (resultMap.containsKey(ind)) continue;
			final var ip = indicatorToIpMap.get(ind);
			if (ip != null && ipCache.containsKey(ip))
				resultMap.put(ind, ipCache.get(ip));
			else
				resultMap.put(ind, GeoLocationDto.unknown(ind, "Unresolved"));
		}

		return resultMap;
	}

	@Override
	public String extractHostOrIp(final String indicator, final IndicatorType indicatorType) {
		if (indicator == null || indicator.isBlank()) return null;
		var text = indicator.trim();

		// Strip leading protocol if present
		if (text.startsWith("http://") || text.startsWith("https://")) {
			try {
				final var uri = URI.create(text);
				final var host = uri.getHost();
				if (host != null && !host.isBlank()) return host;
			} catch (final Exception ignored) {
				// Fall back to regex/string extraction
			}
			text = text.replaceFirst("^https?://", "");
		}

		// Strip port or path if attached
		final var slashIdx = text.indexOf('/');
		if (slashIdx != -1)
			text = text.substring(0, slashIdx);

		if (text.startsWith("[") && text.contains("]")) {
			final var endBracket = text.indexOf(']');
			text = text.substring(1, endBracket);
		} else if (!text.contains(":") || text.indexOf(':') == text.lastIndexOf(':')) {
			final var colonIdx = text.indexOf(':');
			if (colonIdx != -1)
				text = text.substring(0, colonIdx);
		}

		return text.isBlank() ? null : text;
	}

	private GeoLocationDto mapEntityToDto(final IpGeolocationEntity entity) {
		return GeoLocationDto.builder()
				.ip(entity.getIp())
				.countryCode(entity.getCountryCode())
				.countryName(entity.getCountryName())
				.region(entity.getRegion())
				.city(entity.getCity())
				.latitude(entity.getLatitude())
				.longitude(entity.getLongitude())
				.asn(entity.getAsn())
				.organization(entity.getOrganization())
				.geoSource(entity.getSource())
				.build();
	}
}
