package com.cyberthreat.dashboard.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ThreatApiConstants {

	// ==========================================
	// IP Geolocation API Constants
	// ==========================================
	public static final String IPAPI_BASE_URL = "https://ipapi.co";

	// ==========================================
	// Investigation Deep-Link & Portal URLs
	// ==========================================
	public static final String VIRUSTOTAL_SEARCH_URL = "https://www.virustotal.com/gui/search/";
	public static final String URLHAUS_SEARCH_URL = "https://urlhaus.abuse.ch/browse.php?search=";
	public static final String THREATFOX_SEARCH_URL = "https://threatfox.abuse.ch/browse.php?search=ioc%3A";
	public static final String THREATFOX_IOC_URL = "https://threatfox.abuse.ch/ioc/";
	public static final String OPENPHISH_SEARCH_URL = "https://openphish.com/?url=";

	// ==========================================
	// Default External Threat Feed URLs
	// ==========================================
	public static final String URLHAUS_DEFAULT_FEED_URL = "https://urlhaus.abuse.ch/downloads/json_recent/";
	public static final String THREATFOX_DEFAULT_FEED_URL = "https://threatfox.abuse.ch/export/json/recent/";
	public static final String OPENPHISH_DEFAULT_FEED_URL = "https://raw.githubusercontent.com/openphish/public_feed/refs/heads/main/feed.txt";
	public static final String TWEETFEED_DEFAULT_FEED_URL = "https://api.tweetfeed.live/v1/month";
	public static final String TWEETFEED_SEARCH_URL = "https://tweetfeed.live";
}
