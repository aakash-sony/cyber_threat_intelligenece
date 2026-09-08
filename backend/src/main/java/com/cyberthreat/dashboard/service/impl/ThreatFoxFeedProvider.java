package com.cyberthreat.dashboard.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.cyberthreat.dashboard.constant.ThreatApiConstants;
import com.cyberthreat.dashboard.dto.request.ThreatCreateRequest;
import com.cyberthreat.dashboard.enums.IndicatorType;
import com.cyberthreat.dashboard.enums.Severity;
import com.cyberthreat.dashboard.enums.ThreatStatus;
import com.cyberthreat.dashboard.enums.ThreatType;
import com.cyberthreat.dashboard.service.ThreatIntelligenceProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Live Threat Intelligence Provider for ThreatFox (abuse.ch).
 * Ingests multi-indicator IOCs (IPs, domains, URLs, hashes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ThreatFoxFeedProvider implements ThreatIntelligenceProvider {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${threat.feed.threatfox.enabled:true}")
	private boolean enabled;

	@Value("${threat.feed.threatfox.url:https://threatfox.abuse.ch/export/json/recent/}")
	private String feedUrl;

	@Value("${threat.ingestion.batch-limit:2500}")
	private int batchLimit;

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	public String getProviderName() {
		return "ThreatFox";
	}

	@Override
	public String getFeedUrl() {
		return ThreatApiConstants.THREATFOX_SEARCH_URL;
	}

	@Override
	public String getDescription() {
		return "Free platform from abuse.ch to share indicators of compromise (IOCs) associated with malware and botnets.";
	}

	@Override
	public String getProviderType() {
		return "REST JSON IOC Export (abuse.ch)";
	}

	@Override
	public boolean isLiveFeed() {
		return enabled;
	}

	@Override
	public List<ThreatCreateRequest> fetchThreats() {
		return fetchThreats(false);
	}

	@Override
	public List<ThreatCreateRequest> fetchThreats(final boolean isInitialSync) {
		if (!enabled) {
			log.info("ThreatFox live feed provider is disabled in configuration.");
			return Collections.emptyList();
		}

		final List<ThreatCreateRequest> results = new ArrayList<>();
		try {
			final var targetUrl = feedUrl != null && !feedUrl.isBlank() ? feedUrl : ThreatApiConstants.THREATFOX_DEFAULT_FEED_URL;
			log.info("Polling live IOC telemetry from ThreatFox feed: {}", targetUrl);
			final var responseBody = restTemplate.getForObject(targetUrl, String.class);
			if (responseBody == null || responseBody.isBlank()) {
				log.warn("ThreatFox returned an empty response payload.");
				return results;
			}

			final var root = objectMapper.readTree(responseBody);
			final var fields = root.fields();

			var count = 0;
			while (fields.hasNext() && count < batchLimit) {
				final var entry = fields.next();
				final var iocId = entry.getKey();
				final var arrayNode = entry.getValue();

				if (arrayNode != null && arrayNode.isArray() && !arrayNode.isEmpty()) {
					final var item = arrayNode.get(0);
					final var iocValue = item.path("ioc_value").asText("").trim();
					if (iocValue.isBlank()) continue;

					final var iocTypeStr = item.path("ioc_type").asText("url").toLowerCase();
					final var threatTypeStr = item.path("threat_type").asText("payload_delivery").toLowerCase();
					final var malwareName = item.path("malware_printable").asText("Malware Activity");
					final var confidence = item.path("confidence_level").asInt(75);
					final var dateStr = item.path("first_seen_utc").asText("");
					final var lastSeenStr = item.path("last_seen_utc").asText("");

					LocalDateTime firstSeenTimestamp = null;
					if (!dateStr.isBlank()) {
						try {
							firstSeenTimestamp = LocalDateTime.parse(dateStr, DATE_FORMAT);
						} catch (final Exception ignored) {
							// Leave null if unparseable
						}
					}

					LocalDateTime lastSeenTimestamp = null;
					if (!lastSeenStr.isBlank() && !"null".equalsIgnoreCase(lastSeenStr)) {
						try {
							lastSeenTimestamp = LocalDateTime.parse(lastSeenStr, DATE_FORMAT);
						} catch (final Exception ignored) {
							// Leave null if unparseable
						}
					}
					if (lastSeenTimestamp == null) {
						lastSeenTimestamp = firstSeenTimestamp;
					}

					final IndicatorType indType;
					if (iocTypeStr.contains("ip"))
						indType = IndicatorType.IP;
					else if (iocTypeStr.contains("domain"))
						indType = IndicatorType.DOMAIN;
					else if (iocTypeStr.contains("hash") || iocTypeStr.contains("md5") || iocTypeStr.contains("sha"))
						indType = IndicatorType.HASH;
					else
						indType = IndicatorType.URL;

					final ThreatType tType;
					if (threatTypeStr.contains("botnet") || malwareName.toLowerCase().contains("botnet"))
						tType = ThreatType.BOTNET;
					else if (threatTypeStr.contains("c2") || threatTypeStr.contains("command") || malwareName.toLowerCase().contains("c2"))
						tType = ThreatType.COMMAND_AND_CONTROL;
					else if (malwareName.toLowerCase().contains("ransomware"))
						tType = ThreatType.RANSOMWARE;
					else if (threatTypeStr.contains("phishing"))
						tType = ThreatType.PHISHING;
					else
						tType = ThreatType.MALWARE;

					final Severity severity;
					if (confidence >= 85 || tType == ThreatType.COMMAND_AND_CONTROL || tType == ThreatType.RANSOMWARE)
						severity = Severity.CRITICAL;
					else if (confidence >= 70)
						severity = Severity.HIGH;
					else
						severity = Severity.MEDIUM;

					final List<String> tags = new ArrayList<>();
					tags.add("threatfox");
					tags.add(indType.name().toLowerCase());
					tags.add(tType.name().toLowerCase());
					if (item.has("tags")) {
						final var tagsNode = item.get("tags");
						if (tagsNode.isArray()) {
							for (final JsonNode t : tagsNode) {
								if (!t.asText().isBlank()) tags.add(t.asText().trim().toLowerCase());
							}
						} else if (tagsNode.isTextual() && !tagsNode.asText().isBlank()) {
							final var tagTokens = tagsNode.asText().split(",");
							for (final String t : tagTokens) {
								if (!t.isBlank()) tags.add(t.trim().toLowerCase());
							}
						}
					}

					final var sourceUrl = ThreatApiConstants.THREATFOX_IOC_URL + iocId + "/";
					final var description = String.format(
							"%s indicator identified as %s. Threat classification: %s with confidence level of %d%%.",
							malwareName, indType.name(), threatTypeStr.replace('_', ' '), confidence
					);

					var cleanIoc = iocValue.trim();
					if (cleanIoc.length() > 500) {
						cleanIoc = cleanIoc.substring(0, 500);
					}

					// Note: country is left null for authentic IP Geolocation resolution
					final var request = ThreatCreateRequest.builder()
							.indicator(cleanIoc)
							.indicatorType(indType)
							.threatType(tType)
							.severity(severity)
							.confidence(confidence)
							.description(description)
							.source("ThreatFox")
							.sourceUrl(sourceUrl)
							.target(null)
							.country(null)
							.status(ThreatStatus.ACTIVE)
							.tags(tags)
							.firstSeen(firstSeenTimestamp)
							.lastSeen(lastSeenTimestamp)
							.build();

					results.add(request);
					count++;
				}
			}
			log.info("Successfully fetched and normalized {} live IOCs from ThreatFox.", results.size());
		} catch (final Exception e) {
			log.error("Failed to fetch live data from ThreatFox feed: {}", e.getMessage());
		}
		return results;
	}
}
