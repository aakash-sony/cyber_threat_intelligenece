package com.cyberthreat.dashboard.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import com.cyberthreat.dashboard.constant.ThreatApiConstants;
import com.cyberthreat.dashboard.enums.IndicatorType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility for generating validated deep-link investigation portal URLs
 * into verified threat feeds and intelligence repositories.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ThreatUrlUtils {

	private static final Pattern IPV4_PATTERN = Pattern.compile(
			"^(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
	);

	private static final Pattern DOMAIN_PATTERN = Pattern.compile(
			"^[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)+$"
	);

	private static final Pattern HASH_PATTERN = Pattern.compile(
			"^[a-fA-F0-9]{32,64}$"
	);

	private static final Pattern IOC_ID_PATTERN = Pattern.compile(
			"^[a-zA-Z0-9.\\-_]+$"
	);

	/**
	 * Primary entry point for constructing an investigation portal URL for any threat indicator.
	 */
	public static String buildInvestigationUrl(final String source, final String indicator, final IndicatorType indType) {
		if (source == null || source.isBlank())
			return buildVirusTotalUrl(indicator);

		final var src = source.toLowerCase(Locale.ROOT);

		if (src.contains("urlhaus"))
			return buildUrlHausUrl(indicator);
		if (src.contains("threatfox"))
			return buildThreatFoxUrl(indicator);
		if (src.contains("tweetfeed"))
			return "https://tweetfeed.live";
		if (src.contains("openphish"))
			return buildOpenPhishUrl(indicator);
		if (src.contains("phishtank"))
			return buildPhishTankUrl(indicator);
		if (src.contains("alienvault") || src.contains("otx"))
			return buildOtxUrl(indicator, indType);
		if (src.contains("i4c") || src.contains("ncrp") || src.contains("cybercrime"))
			return "https://cybercrime.gov.in/";

		return buildVirusTotalUrl(indicator);
	}

	public static String buildExactCaseUrl(final String source, final String indicator, final IndicatorType indType) {
		return buildInvestigationUrl(source, indicator, indType);
	}

	public static String buildVirusTotalUrl(final String indicator) {
		if (indicator == null || indicator.isBlank())
			return ThreatApiConstants.VIRUSTOTAL_SEARCH_URL;
		return ThreatApiConstants.VIRUSTOTAL_SEARCH_URL + encodeQuery(indicator.trim());
	}

	public static String buildUrlHausUrl(final String indicator) {
		if (indicator == null || indicator.isBlank())
			return "https://urlhaus.abuse.ch/browse/";
		return ThreatApiConstants.URLHAUS_SEARCH_URL + encodeQuery(indicator.trim());
	}

	public static String buildThreatFoxUrl(final String indicator) {
		if (indicator == null || indicator.isBlank())
			return "https://threatfox.abuse.ch/browse/";
		final var trimmed = indicator.trim();
		if (IOC_ID_PATTERN.matcher(trimmed).matches())
			return ThreatApiConstants.THREATFOX_IOC_URL + trimmed + "/";
		return ThreatApiConstants.THREATFOX_SEARCH_URL + encodeQuery(trimmed);
	}

	public static String buildOpenPhishUrl(final String indicator) {
		if (indicator == null || indicator.isBlank())
			return "https://openphish.com/";
		return ThreatApiConstants.OPENPHISH_SEARCH_URL + encodeQuery(indicator.trim());
	}

	public static String buildPhishTankUrl(final String indicator) {
		if (indicator == null || indicator.isBlank())
			return "https://phishtank.org/";
		return "https://phishtank.org/phish_search.php?valid=y&active=y&Search=Search&search_path=" + encodeQuery(indicator.trim());
	}

	public static String buildOtxUrl(final String indicator, final IndicatorType indType) {
		if (indicator == null || indicator.isBlank())
			return "https://otx.alienvault.com/indicator/";
		final var ind = indicator.trim();
		if (indType == IndicatorType.IP || IPV4_PATTERN.matcher(ind).matches())
			return "https://otx.alienvault.com/indicator/ip/" + ind;
		if (indType == IndicatorType.DOMAIN || DOMAIN_PATTERN.matcher(ind).matches())
			return "https://otx.alienvault.com/indicator/domain/" + ind;
		if (indType == IndicatorType.HASH || HASH_PATTERN.matcher(ind).matches())
			return "https://otx.alienvault.com/indicator/file/" + ind;
		return "https://otx.alienvault.com/indicator/url/" + encodeQuery(ind);
	}

	private static String encodeQuery(final String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
