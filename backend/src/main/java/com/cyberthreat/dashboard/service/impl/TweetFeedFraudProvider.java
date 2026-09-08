package com.cyberthreat.dashboard.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
 * Live Threat Intelligence Provider for TweetFeed (api.tweetfeed.live).
 * Provides authentic, community-reported cyber fraud, cryptocurrency scams, wallet drainers,
 * phishing campaigns, and malware telemetry gathered from security researchers on Twitter/X.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TweetFeedFraudProvider implements ThreatIntelligenceProvider {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${threat.feed.tweetfeed.enabled:true}")
	private boolean enabled;

	@Value("${threat.feed.tweetfeed.initial-url:https://api.tweetfeed.live/v1/month}")
	private String initialUrl;

	@Value("${threat.feed.tweetfeed.incremental-url:https://api.tweetfeed.live/v1/today}")
	private String incrementalUrl;

	@Value("${threat.ingestion.batch-limit:2500}")
	private int batchLimit;

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	@Override
	public String getProviderName() {
		return "TweetFeed";
	}

	@Override
	public String getFeedUrl() {
		return "https://tweetfeed.live";
	}

	@Override
	public String getDescription() {
		return "Community-driven open threat intelligence tracking real-time crypto fraud, wallet drainers, scam infrastructure, and malware attacks reported by security researchers on Twitter/X.";
	}

	@Override
	public String getProviderType() {
		return "OSINT Cyber Fraud & Threat Feed (api.tweetfeed.live)";
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
			log.info("TweetFeed live provider is disabled in configuration.");
			return Collections.emptyList();
		}

		final String targetUrl = isInitialSync
				? (initialUrl != null && !initialUrl.isBlank() ? initialUrl : "https://api.tweetfeed.live/v1/month")
				: (incrementalUrl != null && !incrementalUrl.isBlank() ? incrementalUrl : "https://api.tweetfeed.live/v1/today");

		final List<ThreatCreateRequest> results = new ArrayList<>();
		try {
			log.info("Polling live cyber fraud & threat telemetry from TweetFeed [mode: {}]: {}",
					isInitialSync ? "INITIAL" : "INCREMENTAL", targetUrl);

			final HttpHeaders headers = new HttpHeaders();
			headers.set("User-Agent", "CyberThreatIntelligenceDashboard/2.0 (Security Research)");
			final HttpEntity<String> entity = new HttpEntity<>(headers);

			final ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
			final var responseBody = response.getBody();
			if (responseBody == null || responseBody.isBlank()) {
				log.warn("TweetFeed returned an empty response payload.");
				return results;
			}

			final JsonNode root = objectMapper.readTree(responseBody);
			if (!root.isArray()) {
				log.warn("TweetFeed expected JSON array, got {}", root.getNodeType());
				return results;
			}

			// Prioritize fraud / scam indicators while respecting the batch limit
			final List<JsonNode> fraudItems = new ArrayList<>();
			final List<JsonNode> otherItems = new ArrayList<>();
			for (final JsonNode item : root) {
				boolean isFraud = false;
				if (item.has("tags") && item.get("tags").isArray()) {
					for (final JsonNode t : item.get("tags")) {
						final var tag = t.asText("").trim().toLowerCase();
						if (tag.contains("scam") || tag.contains("drainer") || tag.contains("fraud") ||
							tag.contains("cryptoscam") || tag.contains("crypto") || tag.contains("fake")) {
							isFraud = true;
							break;
						}
					}
				}
				if (isFraud) {
					fraudItems.add(item);
				} else {
					otherItems.add(item);
				}
			}

			final List<JsonNode> toProcess = new ArrayList<>(fraudItems);
			final int remainingSlots = Math.max(0, batchLimit - fraudItems.size());
			toProcess.addAll(otherItems.subList(0, Math.min(remainingSlots, otherItems.size())));

			for (final JsonNode item : toProcess) {
				final var indicator = item.path("value").asText("").trim();
				if (indicator.isBlank()) {
					continue;
				}

				final var typeStr = item.path("type").asText("url").toLowerCase().trim();
				final var user = item.path("user").asText("").trim();
				final var tweetUrl = item.path("tweet").asText("").trim();
				final var dateStr = item.path("date").asText("").trim();

				LocalDateTime realTimestamp = null;
				if (!dateStr.isBlank()) {
					try {
						realTimestamp = LocalDateTime.parse(dateStr, DATE_FORMAT);
					} catch (final Exception ignored) {
						// Leave null if unparseable; will fallback to ingestion timestamp
					}
				}

				final IndicatorType indType;
				if (typeStr.contains("ip")) {
					indType = IndicatorType.IP;
				} else if (typeStr.contains("domain")) {
					indType = IndicatorType.DOMAIN;
				} else if (typeStr.contains("sha256") || typeStr.contains("md5") || typeStr.contains("hash")) {
					indType = IndicatorType.HASH;
				} else {
					indType = IndicatorType.URL;
				}

				final List<String> rawTags = new ArrayList<>();
				if (item.has("tags") && item.get("tags").isArray()) {
					for (final JsonNode t : item.get("tags")) {
						var tagText = t.asText("").trim().toLowerCase();
						if (tagText.startsWith("#")) {
							tagText = tagText.substring(1);
						}
						if (!tagText.isBlank()) {
							rawTags.add(tagText);
						}
					}
				}

				// Classify threat type according to authentic researcher tags
				final ThreatType tType;
				final boolean isFraud = rawTags.stream().anyMatch(t ->
					t.contains("scam") || t.contains("drainer") || t.contains("fraud") ||
					t.contains("cryptoscam") || t.contains("crypto") || t.contains("fake"));

				if (isFraud) {
					tType = ThreatType.FRAUD;
				} else if (rawTags.stream().anyMatch(t -> t.contains("phish"))) {
					tType = ThreatType.PHISHING;
				} else if (rawTags.stream().anyMatch(t -> t.contains("ransom"))) {
					tType = ThreatType.RANSOMWARE;
				} else if (rawTags.stream().anyMatch(t -> t.contains("c2") || t.contains("command"))) {
					tType = ThreatType.COMMAND_AND_CONTROL;
				} else if (rawTags.stream().anyMatch(t -> t.contains("botnet"))) {
					tType = ThreatType.BOTNET;
				} else {
					tType = ThreatType.MALWARE;
				}

				// Assign authentic severity
				final Severity severity;
				final boolean isAptOrHighProfile = rawTags.stream().anyMatch(t ->
					t.contains("apt") || t.contains("kimsuky") || t.contains("lazarus") || t.contains("dprk"));
				if (tType == ThreatType.RANSOMWARE || tType == ThreatType.COMMAND_AND_CONTROL || isAptOrHighProfile) {
					severity = Severity.CRITICAL;
				} else if (tType == ThreatType.FRAUD || tType == ThreatType.PHISHING) {
					severity = Severity.HIGH;
				} else {
					severity = Severity.MEDIUM;
				}

				final List<String> tags = new ArrayList<>();
				tags.add("tweetfeed");
				tags.add(indType.name().toLowerCase());
				tags.add(tType.name().toLowerCase());
				tags.addAll(rawTags);
				if (!user.isBlank()) {
					tags.add("author:" + user);
				}

				final var sourceUrl = !tweetUrl.isBlank() ? tweetUrl : "https://tweetfeed.live";
				final String description;
				if (tType == ThreatType.FRAUD) {
					description = String.format("Cryptocurrency fraud / scam campaign identified by OSINT researcher @%s. Indicator: %s",
							!user.isBlank() ? user : "OSINT", indicator);
				} else {
					description = String.format("%s activity reported by security researcher @%s on Twitter/X. Threat classification: %s.",
							indType.name(), !user.isBlank() ? user : "OSINT", tType.name());
				}

				var cleanIndicator = indicator.trim();
				if (cleanIndicator.length() > 500) {
					cleanIndicator = cleanIndicator.substring(0, 500);
				}

				final var request = ThreatCreateRequest.builder()
						.indicator(cleanIndicator)
						.indicatorType(indType)
						.threatType(tType)
						.severity(severity)
						.status(ThreatStatus.ACTIVE)
						.confidence(85)
						.source("TweetFeed")
						.sourceUrl(sourceUrl)
						.description(description)
						.country(null) // Left null for authentic Geolocation resolution
						.firstSeen(realTimestamp)
						.lastSeen(realTimestamp)
						.target(tType == ThreatType.FRAUD ? "Cryptocurrency / Web3 Users" : null)
						.tags(tags)
						.build();

				results.add(request);
			}

			log.info("Fetched and normalized {} authentic threat records from TweetFeed OSINT API.", results.size());
		} catch (final Exception e) {
			log.error("Failed to fetch or parse TweetFeed telemetry: {}", e.getMessage(), e);
		}

		return results;
	}
}
