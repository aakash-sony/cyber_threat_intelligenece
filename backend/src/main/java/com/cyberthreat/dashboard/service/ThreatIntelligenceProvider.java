package com.cyberthreat.dashboard.service;

import java.util.List;

import com.cyberthreat.dashboard.dto.request.ThreatCreateRequest;

/**
 * Common contract for external threat intelligence providers and live feeds.
 */
public interface ThreatIntelligenceProvider {

	/**
	 * Provider name identifier (e.g., URLhaus, ThreatFox, TweetFeed, OpenPhish)
	 */
	String getProviderName();

	/**
	 * Fetch threat records from provider feed (default/incremental)
	 */
	List<ThreatCreateRequest> fetchThreats();

	/**
	 * Fetch threat records distinguishing initial application boot synchronization
	 * from subsequent 15-minute incremental synchronization cycles.
	 */
	default List<ThreatCreateRequest> fetchThreats(final boolean isInitialSync) {
		return fetchThreats();
	}

	/**
	 * Whether this provider connects to a live external feed
	 */
	default boolean isLiveFeed() {
		return true;
	}

	/**
	 * Official website or feed endpoint URL
	 */
	default String getFeedUrl() {
		return "";
	}

	/**
	 * Human-readable description of this provider
	 */
	default String getDescription() {
		return "Threat intelligence provider feed";
	}

	/**
	 * Provider intelligence type
	 */
	default String getProviderType() {
		return "REST Webhook & Periodic Poll";
	}
}
