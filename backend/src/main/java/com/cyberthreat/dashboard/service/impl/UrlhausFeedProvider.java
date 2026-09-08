package com.cyberthreat.dashboard.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import com.cyberthreat.dashboard.util.ThreatUrlUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Live Threat Intelligence Provider for URLhaus (abuse.ch).
 * Ingests authentic malware distribution URLs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UrlhausFeedProvider implements ThreatIntelligenceProvider {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${threat.feed.urlhaus.enabled:true}")
	private boolean enabled;

	@Value("${threat.feed.urlhaus.url:https://urlhaus.abuse.ch/downloads/json_recent/}")
	private String feedUrl;

	@Value("${threat.ingestion.batch-limit:2500}")
	private int batchLimit;

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

	@Override
	public String getProviderName() {
		return "URLhaus";
	}

	@Override
	public String getFeedUrl() {
		return ThreatApiConstants.URLHAUS_SEARCH_URL;
	}

	@Override
	public String getDescription() {
		return "Project from abuse.ch sharing live malicious URLs used for payload and malware distribution.";
	}

	@Override
	public String getProviderType() {
		return "REST JSON Feed (abuse.ch)";
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
			log.info("URLhaus live feed provider is disabled in configuration.");
			return Collections.emptyList();
		}

		final List<ThreatCreateRequest> results = new ArrayList<>();
		try {
			final String targetUrl = (feedUrl != null && !feedUrl.isBlank()) ? feedUrl : ThreatApiConstants.URLHAUS_DEFAULT_FEED_URL;
			log.info("Polling live malware telemetry from URLhaus feed: {}", targetUrl);
			final String responseBody = restTemplate.getForObject(targetUrl, String.class);
			if (responseBody == null || responseBody.isBlank()) {
				log.warn("URLhaus returned an empty response payload.");
				return results;
			}

			final JsonNode root = objectMapper.readTree(responseBody);
			final Iterator<Map.Entry<String, JsonNode>> fields = root.fields();

			int count = 0;
			while (fields.hasNext() && count < batchLimit) {
				final Map.Entry<String, JsonNode> entry = fields.next();
				final JsonNode arrayNode = entry.getValue();

				if (arrayNode != null && arrayNode.isArray() && !arrayNode.isEmpty()) {
					final JsonNode item = arrayNode.get(0);
					final String url = item.path("url").asText("");
					if (url.isBlank()) continue;

					final String urlStatus = item.path("url_status").asText("offline");
					final String threatName = item.path("threat").asText("malware_download");
					final String reporter = item.path("reporter").asText("anonymous");
					final String rawUrlhausLink = item.path("urlhaus_link").asText("");
					final String urlhausLink = (!rawUrlhausLink.isBlank() && !rawUrlhausLink.endsWith("/browse"))
							? rawUrlhausLink
							: ThreatUrlUtils.buildUrlHausUrl(url.trim());
					final String dateStr = item.path("dateadded").asText("");
					final String lastOnlineStr = item.path("last_online").asText("");

					LocalDateTime firstSeenTimestamp = null;
					if (!dateStr.isBlank()) {
						try {
							firstSeenTimestamp = LocalDateTime.parse(dateStr, DATE_FORMAT);
						} catch (final Exception ignored) {
							// Leave null if unparseable
						}
					}

					LocalDateTime lastSeenTimestamp = null;
					if (!lastOnlineStr.isBlank()) {
						try {
							lastSeenTimestamp = LocalDateTime.parse(lastOnlineStr, DATE_FORMAT);
						} catch (final Exception ignored) {
							// Leave null if unparseable
						}
					}
					if (lastSeenTimestamp == null) {
						lastSeenTimestamp = firstSeenTimestamp;
					}

					final List<String> tags = new ArrayList<>();
					tags.add("malware");
					tags.add("urlhaus");
					if (item.has("tags") && item.get("tags").isArray()) {
						for (final JsonNode t : item.get("tags")) {
							if (!t.asText().isBlank()) tags.add(t.asText().trim().toLowerCase());
						}
					}

					Severity severity = "online".equalsIgnoreCase(urlStatus) ? Severity.HIGH : Severity.MEDIUM;
					if (tags.contains("elf") || tags.contains("mirai") || tags.contains("mozi") || tags.contains("trojan")) {
						severity = Severity.CRITICAL;
					}

					final String description = String.format(
							"Malware distribution endpoint. Threat: %s. Reporter: %s. Operational status: %s.",
							threatName, reporter, urlStatus.toUpperCase()
					);

					String cleanUrl = url.trim();
					if (cleanUrl.length() > 500) {
						cleanUrl = cleanUrl.substring(0, 500);
					}

					// Note: country is left null for authentic IP Geolocation resolution
					final ThreatCreateRequest request = ThreatCreateRequest.builder()
							.indicator(cleanUrl)
							.indicatorType(IndicatorType.URL)
							.threatType(ThreatType.MALWARE)
							.severity(severity)
							.confidence(severity == Severity.CRITICAL ? 95 : 88)
							.description(description)
							.source("URLhaus")
							.sourceUrl(urlhausLink)
							.target(null)
							.country(null)
							.status("online".equalsIgnoreCase(urlStatus) ? ThreatStatus.ACTIVE : ThreatStatus.RESOLVED)
							.tags(tags)
							.firstSeen(firstSeenTimestamp)
							.lastSeen(lastSeenTimestamp)
							.build();

					results.add(request);
					count++;
				}
			}
			log.info("Successfully fetched and normalized {} live malware indicators from URLhaus.", results.size());
		} catch (final Exception e) {
			log.error("Failed to fetch live data from URLhaus feed: {}", e.getMessage());
		}
		return results;
	}
}
