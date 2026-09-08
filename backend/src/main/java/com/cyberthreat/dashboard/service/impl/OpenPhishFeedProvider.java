package com.cyberthreat.dashboard.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.cyberthreat.dashboard.util.ThreatUrlUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Live Threat Intelligence Provider for OpenPhish (Community feed.txt).
 *
 * PROVIDER LIMITATION:
 * The OpenPhish free public feed delivers a raw text list of currently active phishing URLs.
 * It does NOT supply historical records, publication timestamps, confidence ratings, or target brand metadata.
 * Consequently, the timestamps recorded represent the initial observation time when this system first
 * and last ingested the URL from the active feed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenPhishFeedProvider implements ThreatIntelligenceProvider {

	private final RestTemplate restTemplate;

	@Value("${threat.feed.openphish.enabled:true}")
	private boolean enabled;

	@Value("${threat.feed.openphish.url:https://raw.githubusercontent.com/openphish/public_feed/refs/heads/main/feed.txt}")
	private String feedUrl;

	@Value("${threat.ingestion.batch-limit:2500}")
	private int batchLimit;

	@Override
	public String getProviderName() {
		return "OpenPhish";
	}

	@Override
	public String getFeedUrl() {
		return ThreatApiConstants.OPENPHISH_SEARCH_URL;
	}

	@Override
	public String getDescription() {
		return "Automated phishing intelligence platform identifying streaming phishing URLs.";
	}

	@Override
	public String getProviderType() {
		return "Real-time Text Feed";
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
			log.info("OpenPhish live feed provider is disabled in configuration.");
			return Collections.emptyList();
		}

		final List<ThreatCreateRequest> results = new ArrayList<>();
		try {
			final var targetUrl = feedUrl != null && !feedUrl.isBlank() ? feedUrl : ThreatApiConstants.OPENPHISH_DEFAULT_FEED_URL;
			log.info("Polling live phishing telemetry from OpenPhish feed: {}", targetUrl);
			final var responseBody = restTemplate.getForObject(targetUrl, String.class);
			if (responseBody == null || responseBody.isBlank()) {
				log.warn("OpenPhish returned an empty response payload.");
				return results;
			}

			final var lines = responseBody.split("\\r?\\n");
			var count = 0;
			for (final String line : lines) {
				if (count >= batchLimit) break;
				final var url = line != null ? line.trim() : "";
				var cleanUrl = url;
				if (cleanUrl.length() > 500) {
					cleanUrl = cleanUrl.substring(0, 500);
				}

				final var sourceUrl = ThreatUrlUtils.buildOpenPhishUrl(cleanUrl);
				final List<String> tags = Arrays.asList("phishing", "credential-harvesting", "openphish", "active");

				// Note: country and timestamps are null since OpenPhish text feed does not provide them
				final var request = ThreatCreateRequest.builder()
						.indicator(cleanUrl)
						.indicatorType(IndicatorType.URL)
						.threatType(ThreatType.PHISHING)
						.severity(Severity.HIGH)
						.confidence(90)
						.description("Phishing URL identified by OpenPhish automated detection stream.")
						.source("OpenPhish")
						.sourceUrl(sourceUrl)
						.target(null)
						.country(null)
						.status(ThreatStatus.ACTIVE)
						.tags(tags)
						.firstSeen(null)
						.lastSeen(null)
						.build();

				results.add(request);
				count++;
			}
			log.info("Successfully fetched and normalized {} live phishing indicators from OpenPhish.", results.size());
		} catch (final Exception e) {
			log.error("Failed to fetch live data from OpenPhish feed: {}", e.getMessage());
		}
		return results;
	}
}
