package com.cyberthreat.dashboard.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cyberthreat.dashboard.dto.response.ActivityPointResponse;
import com.cyberthreat.dashboard.dto.response.DashboardSummaryResponse;
import com.cyberthreat.dashboard.dto.response.DetailedSourceResponse;
import com.cyberthreat.dashboard.dto.response.SeverityStatResponse;
import com.cyberthreat.dashboard.dto.response.SourceStatResponse;
import com.cyberthreat.dashboard.dto.response.ThreatTypeStatResponse;
import com.cyberthreat.dashboard.entity.ThreatEntity;
import com.cyberthreat.dashboard.enums.IndicatorType;
import com.cyberthreat.dashboard.enums.Severity;
import com.cyberthreat.dashboard.enums.ThreatStatus;
import com.cyberthreat.dashboard.enums.ThreatType;
import com.cyberthreat.dashboard.repository.ThreatRepository;
import com.cyberthreat.dashboard.service.DashboardService;
import com.cyberthreat.dashboard.service.ThreatIntelligenceProvider;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

	private final ThreatRepository threatRepository;
	private final List<ThreatIntelligenceProvider> providers;

	private static final Map<String, String> SOURCE_COLORS = Map.of(
			"URLhaus", "#8b5cf6",
			"ThreatFox", "#ef4444",
			"TweetFeed", "#0284c7",
			"OpenPhish", "#059669"
			);

	@Override
	@Transactional(readOnly = true)
	public DashboardSummaryResponse getSummary(final String timeRange, final String country) {
		final var startTime = parseStartTime(timeRange);
		final var normalizedCountry = normalizeCountry(country);
		final var startOfDay = LocalDate.now().atStartOfDay();

		final var summaryRows = threatRepository.getDashboardSummaryMetrics(normalizedCountry, startTime);
		long totalThreats = 0;
		long criticalThreats = 0;
		long highThreats = 0;
		long mediumThreats = 0;
		long lowThreats = 0;
		long activeThreats = 0;
		long phishingCount = 0;
		long malwareCount = 0;
		long fraudCount = 0;
		long maliciousUrls = 0;
		long maliciousIps = 0;
		long domains = 0;
		long hashes = 0;

		if (summaryRows != null && !summaryRows.isEmpty()) {
			final Object[] row = summaryRows.get(0);
			totalThreats = row[0] != null ? ((Number) row[0]).longValue() : 0L;
			criticalThreats = row[1] != null ? ((Number) row[1]).longValue() : 0L;
			highThreats = row[2] != null ? ((Number) row[2]).longValue() : 0L;
			mediumThreats = row[3] != null ? ((Number) row[3]).longValue() : 0L;
			lowThreats = row[4] != null ? ((Number) row[4]).longValue() : 0L;
			activeThreats = row[5] != null ? ((Number) row[5]).longValue() : 0L;
			phishingCount = row[6] != null ? ((Number) row[6]).longValue() : 0L;
			malwareCount = row[7] != null ? ((Number) row[7]).longValue() : 0L;
			fraudCount = row[8] != null ? ((Number) row[8]).longValue() : 0L;
			maliciousUrls = row[9] != null ? ((Number) row[9]).longValue() : 0L;
			maliciousIps = row[10] != null ? ((Number) row[10]).longValue() : 0L;
			domains = row[11] != null ? ((Number) row[11]).longValue() : 0L;
			hashes = row[12] != null ? ((Number) row[12]).longValue() : 0L;
		}

		final var incRows = threatRepository.getTodayIncrements(normalizedCountry, startOfDay);
		long todayTotalIncrease = 0;
		long todayPhishingIncrease = 0;
		long todayMalwareIncrease = 0;
		long todayFraudIncrease = 0;

		if (incRows != null && !incRows.isEmpty()) {
			final Object[] row = incRows.get(0);
			todayTotalIncrease = row[0] != null ? ((Number) row[0]).longValue() : 0L;
			todayPhishingIncrease = row[1] != null ? ((Number) row[1]).longValue() : 0L;
			todayMalwareIncrease = row[2] != null ? ((Number) row[2]).longValue() : 0L;
			todayFraudIncrease = row[3] != null ? ((Number) row[3]).longValue() : 0L;
		}

		return DashboardSummaryResponse.builder()
				.totalThreats(totalThreats)
				.criticalThreats(criticalThreats)
				.highThreats(highThreats)
				.mediumThreats(mediumThreats)
				.lowThreats(lowThreats)
				.activeThreats(activeThreats)
				.phishingCount(phishingCount)
				.malwareCount(malwareCount)
				.fraudCount(fraudCount)
				.maliciousUrls(maliciousUrls)
				.maliciousIps(maliciousIps)
				.domains(domains)
				.hashes(hashes)
				.todayTotalIncrease(todayTotalIncrease)
				.todayPhishingIncrease(todayPhishingIncrease)
				.todayMalwareIncrease(todayMalwareIncrease)
				.todayFraudIncrease(todayFraudIncrease)
				.build();
	}

	private long countThreatsInWindow(final ThreatType type, final String country, final LocalDateTime start, final LocalDateTime end) {
		final Specification<ThreatEntity> spec = (root, query, cb) -> {
			final List<Predicate> predicates = new ArrayList<>();
			final Expression<LocalDateTime> timestampExpr = cb.<LocalDateTime>coalesce(root.get("lastSeen"), root.get("createdAt"));
			if (start != null)
				predicates.add(cb.greaterThanOrEqualTo(timestampExpr, start));
			if (end != null)
				predicates.add(cb.lessThan(timestampExpr, end));
			if (country != null && !country.isBlank())
				predicates.add(cb.equal(cb.lower(root.get("country")), country.toLowerCase().trim()));
			if (type != null)
				predicates.add(cb.equal(root.get("threatType"), type));
			return cb.and(predicates.toArray(new Predicate[0]));
		};
		return threatRepository.count(spec);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ActivityPointResponse> getActivityTimeline(final String timeRange, final String country) {
		final var range = timeRange == null || timeRange.isBlank() ? "24h" : timeRange.toLowerCase().trim();
		final var normalizedCountry = normalizeCountry(country);
		final var now = LocalDateTime.now();
		final List<ActivityPointResponse> timeline = new ArrayList<>();

		switch (range) {
		case "all":
		case "all time": {
			var minDate = normalizedCountry != null
					? threatRepository.findMinLastSeenWithCountry(normalizedCountry.toLowerCase())
					: threatRepository.findMinLastSeen();
			if (minDate == null) {
				minDate = normalizedCountry != null
						? threatRepository.findMinCreatedAtWithCountry(normalizedCountry.toLowerCase())
						: threatRepository.findMinCreatedAt();
			}
			if (minDate == null)
				return Collections.emptyList();
			final var days = Duration.between(minDate, now).toDays();
			if (days <= 1) {
				final var hFmt = DateTimeFormatter.ofPattern("h a");
				for (var i = 7; i >= 0; i--) {
					final var start = now.minusHours((i + 1) * 3);
					final var end = now.minusHours(i * 3);
					timeline.add(buildActivityPoint(end.format(hFmt), start, end, normalizedCountry));
				}
			} else if (days <= 7) {
				final var dFmt = DateTimeFormatter.ofPattern("d MMM");
				for (var i = (int) days; i >= 0; i--) {
					final var day = now.toLocalDate().minusDays(i);
					final var start = day.atStartOfDay();
					final var end = day.plusDays(1).atStartOfDay();
					timeline.add(buildActivityPoint(day.format(dFmt), start, end, normalizedCountry));
				}
			} else if (days <= 90) {
				final var dFmt = DateTimeFormatter.ofPattern("d MMM");
				final var points = Math.min(18, Math.max(6, (int) (days / 5)));
				final var stepDays = Math.max(1, days / points);
				for (var i = points - 1; i >= 0; i--) {
					final var start = now.minusDays((i + 1) * stepDays);
					final var end = now.minusDays(i * stepDays);
					timeline.add(buildActivityPoint(end.format(dFmt), start, end, normalizedCountry));
				}
			} else {
				final var mFmt = DateTimeFormatter.ofPattern("MMM yyyy");
				var current = minDate.withDayOfMonth(1).toLocalDate().atStartOfDay();
				while (!current.isAfter(now)) {
					final var monthStart = current;
					final var monthEnd = current.plusMonths(1).isAfter(now) ? now : current.plusMonths(1);
					timeline.add(buildActivityPoint(monthStart.format(mFmt), monthStart, monthEnd, normalizedCountry));
					current = current.plusMonths(1);
				}
			}
			break;
		}
		case "1y":
		case "year": {
			final var mFmt = DateTimeFormatter.ofPattern("MMM yyyy");
			for (var i = 11; i >= 0; i--) {
				final var yearMonth = now.minusMonths(i);
				final var monthStart = yearMonth.withDayOfMonth(1).toLocalDate().atStartOfDay();
				final var monthEnd = i == 0 ? now : yearMonth.plusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay();
				timeline.add(buildActivityPoint(monthStart.format(mFmt), monthStart, monthEnd, normalizedCountry));
			}
			break;
		}
		case "7d":
		case "week": {
			final var dFmt = DateTimeFormatter.ofPattern("d MMM");
			for (var i = 6; i >= 0; i--) {
				final var day = now.toLocalDate().minusDays(i);
				final var start = day.atStartOfDay();
				final var end = day.plusDays(1).atStartOfDay();
				timeline.add(buildActivityPoint(day.format(dFmt), start, end, normalizedCountry));
			}
			break;
		}
		case "30d":
		case "1m":
		case "month": {
			final var dFmt = DateTimeFormatter.ofPattern("d MMM");
			for (var i = 5; i >= 0; i--) {
				final var start = now.minusDays((i + 1) * 5);
				final var end = now.minusDays(i * 5);
				timeline.add(buildActivityPoint(end.format(dFmt), start, end, normalizedCountry));
			}
			break;
		}
		case "90d":
		case "3m":
		case "3 months": {
			final var dFmt = DateTimeFormatter.ofPattern("d MMM");
			for (var i = 5; i >= 0; i--) {
				final var start = now.minusDays((i + 1) * 15);
				final var end = now.minusDays(i * 15);
				timeline.add(buildActivityPoint(end.format(dFmt), start, end, normalizedCountry));
			}
			break;
		}
		case "24h":
		default: {
			final var hFmt = DateTimeFormatter.ofPattern("h a");
			for (var i = 7; i >= 0; i--) {
				final var start = now.minusHours((i + 1) * 3);
				final var end = now.minusHours(i * 3);
				timeline.add(buildActivityPoint(end.format(hFmt), start, end, normalizedCountry));
			}
			break;
		}
		}

		return timeline;
	}

	private ActivityPointResponse buildActivityPoint(final String label, final LocalDateTime start, final LocalDateTime end, final String country) {
		final var phishing = countThreatsInWindow(ThreatType.PHISHING, country, start, end);
		final var malware = countThreatsInWindow(ThreatType.MALWARE, country, start, end);
		final var fraud = countThreatsInWindow(ThreatType.FRAUD, country, start, end);
		final var total = countThreatsInWindow(null, country, start, end);

		return ActivityPointResponse.builder()
				.time(label)
				.phishing(phishing)
				.malware(malware)
				.fraud(fraud)
				.total(total)
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<SourceStatResponse> getThreatsBySource(final String timeRange, final String country) {
		final var startTime = parseStartTime(timeRange);
		final var normalizedCountry = normalizeCountry(country);
		List<Object[]> rows;

		if (normalizedCountry != null && startTime != null)
			rows = threatRepository.countGroupedBySourceWithCountryAndSince(normalizedCountry.toLowerCase(), startTime);
		else if (normalizedCountry != null)
			rows = threatRepository.countGroupedBySourceWithCountry(normalizedCountry.toLowerCase());
		else if (startTime != null)
			rows = threatRepository.countGroupedBySourceSince(startTime);
		else
			rows = threatRepository.countGroupedBySource();

		var total = 0L;
		for (final Object[] row : rows)
			total += ((Number) row[1]).longValue();

		final List<SourceStatResponse> result = new ArrayList<>();
		for (final Object[] row : rows) {
			final var source = (String) row[0];
			final var count = ((Number) row[1]).longValue();
			final var percentage = total > 0 ? Math.round(count * 100.0 / total * 10.0) / 10.0 : 0.0;
			final var color = SOURCE_COLORS.getOrDefault(source, "#2563eb");

			result.add(SourceStatResponse.builder()
					.source(source)
					.count(count)
					.percentage(percentage)
					.color(color)
					.build());
		}

		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<SeverityStatResponse> getThreatsBySeverity(final String timeRange, final String country) {
		final var startTime = parseStartTime(timeRange);
		final var normalizedCountry = normalizeCountry(country);
		List<Object[]> rows;

		if (normalizedCountry != null && startTime != null)
			rows = threatRepository.countGroupedBySeverityWithCountryAndSince(normalizedCountry.toLowerCase(), startTime);
		else if (normalizedCountry != null)
			rows = threatRepository.countGroupedBySeverityWithCountry(normalizedCountry.toLowerCase());
		else if (startTime != null)
			rows = threatRepository.countGroupedBySeveritySince(startTime);
		else
			rows = threatRepository.countGroupedBySeverity();

		var total = 0L;
		final Map<Severity, Long> counts = new EnumMap<>(Severity.class);
		for (final Object[] row : rows) {
			if (row[0] == null) continue;
			Severity sev;
			if (row[0] instanceof Severity)
				sev = (Severity) row[0];
			else
				try {
					sev = Severity.valueOf(row[0].toString());
				} catch (final Exception e) {
					continue;
				}
			final var count = ((Number) row[1]).longValue();
			counts.put(sev, count);
			total += count;
		}

		final List<SeverityStatResponse> result = new ArrayList<>();
		for (final Severity sev : Severity.values()) {
			final long count = counts.getOrDefault(sev, 0L);
			final var percentage = total > 0 ? Math.round(count * 100.0 / total * 10.0) / 10.0 : 0.0;
			result.add(SeverityStatResponse.builder()
					.severity(sev)
					.count(count)
					.percentage(percentage)
					.build());
		}

		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ThreatTypeStatResponse> getThreatsByType(final String timeRange, final String country) {
		final var startTime = parseStartTime(timeRange);
		final var normalizedCountry = normalizeCountry(country);
		List<Object[]> rows;

		if (normalizedCountry != null && startTime != null)
			rows = threatRepository.countGroupedByThreatTypeWithCountryAndSince(normalizedCountry.toLowerCase(), startTime);
		else if (normalizedCountry != null)
			rows = threatRepository.countGroupedByThreatTypeWithCountry(normalizedCountry.toLowerCase());
		else if (startTime != null)
			rows = threatRepository.countGroupedByThreatTypeSince(startTime);
		else
			rows = threatRepository.countGroupedByThreatType();

		var total = 0L;
		for (final Object[] row : rows)
			total += ((Number) row[1]).longValue();

		final List<ThreatTypeStatResponse> result = new ArrayList<>();
		for (final Object[] row : rows) {
			if (row[0] == null) continue;
			ThreatType type;
			if (row[0] instanceof ThreatType)
				type = (ThreatType) row[0];
			else
				try {
					type = ThreatType.valueOf(row[0].toString());
				} catch (final Exception e) {
					continue;
				}
			final var count = ((Number) row[1]).longValue();
			final var percentage = total > 0 ? Math.round(count * 100.0 / total * 10.0) / 10.0 : 0.0;

			result.add(ThreatTypeStatResponse.builder()
					.threatType(type)
					.count(count)
					.percentage(percentage)
					.build());
		}

		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> getAvailableCountries() {
		return threatRepository.findDistinctCountries();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getCountryStats(final String timeRange) {
		final var startTime = parseStartTime(timeRange);
		List<Object[]> rows;
		if (startTime != null)
			rows = threatRepository.countGroupedByCountrySince(startTime);
		else
			rows = threatRepository.countGroupedByCountry();

		var total = 0L;
		for (final Object[] row : rows)
			total += ((Number) row[1]).longValue();

		final List<Map<String, Object>> results = new ArrayList<>();
		for (final Object[] row : rows) {
			final var countryName = (String) row[0];
			final var count = ((Number) row[1]).longValue();
			final var percentage = total > 0 ? Math.round(count * 100.0 / total * 10.0) / 10.0 : 0.0;

			final Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("country", countryName);
			entry.put("count", count);
			entry.put("percentage", percentage);
			results.add(entry);
		}
		return results;
	}

	@Override
	@Transactional(readOnly = true)
	public List<DetailedSourceResponse> getDetailedSources() {
		final var sources = threatRepository.findDistinctSources();
		final List<DetailedSourceResponse> result = new ArrayList<>();

		final Map<String, ThreatIntelligenceProvider> providerMap = new HashMap<>();
		if (providers != null)
			for (final ThreatIntelligenceProvider provider : providers)
				providerMap.put(provider.getProviderName(), provider);

		final Set<String> allSourceNames = new LinkedHashSet<>();
		if (providers != null)
			for (final ThreatIntelligenceProvider provider : providers)
				allSourceNames.add(provider.getProviderName());
		allSourceNames.addAll(sources);

		final var grandTotal = threatRepository.count();

		// Pre-aggregate counts and latest detection dates in 2 single queries (no queries in loop)
		final Map<String, Long> sourceCounts = new HashMap<>();
		for (final Object[] row : threatRepository.countGroupedBySource()) {
			if (row != null && row[0] != null && row[1] != null) {
				sourceCounts.put((String) row[0], ((Number) row[1]).longValue());
			}
		}

		final Map<String, LocalDateTime> sourceMaxDates = new HashMap<>();
		for (final Object[] row : threatRepository.findMaxLastSeenGroupedBySource()) {
			if (row != null && row[0] != null && row[1] != null) {
				sourceMaxDates.put((String) row[0], (LocalDateTime) row[1]);
			}
		}

		for (final String source : allSourceNames) {
			final var provider = providerMap.get(source);
			final var total = sourceCounts.getOrDefault(source, 0L);
			final var maxLastSeen = sourceMaxDates.get(source);
			final var isLive = provider != null && provider.isLiveFeed();
			final var status = isLive ? "ONLINE" : total > 0 ? "INACTIVE" : "DISABLED";
			final var desc = provider != null ? provider.getDescription() : "External threat intelligence stream.";
			final var type = provider != null ? provider.getProviderType() : "Threat Intelligence Feed";
			final var url = provider != null && !provider.getFeedUrl().isBlank() ? provider.getFeedUrl() : "";
			final var pct = grandTotal > 0 ? Math.round(total * 100.0 / grandTotal * 10.0) / 10.0 : 0.0;

			result.add(DetailedSourceResponse.builder()
					.name(source)
					.status(status)
					.count(total)
					.percentage(pct)
					.type(type)
					.url(url)
					.color(SOURCE_COLORS.getOrDefault(source, "#2563eb"))
					.lastDetected(maxLastSeen) // Real date from DB or null; NEVER fake LocalDateTime.now()
					.description(desc)
					.build());
		}

		return result;
	}

	private String normalizeCountry(final String country) {
		if (country == null || country.isBlank() || "all".equalsIgnoreCase(country) || "all countries".equalsIgnoreCase(country))
			return null;
		return country.trim().toLowerCase();
	}

	private LocalDateTime parseStartTime(final String timeRange) {
		if (timeRange == null || timeRange.isBlank())
			return null;
		final var now = LocalDateTime.now();
		return switch (timeRange.toLowerCase().trim()) {
		case "24h" -> now.minusHours(24);
		case "7d", "week" -> now.minusDays(7);
		case "30d", "1m", "month" -> now.minusDays(30);
		case "90d", "3m", "3 months" -> now.minusDays(90);
		case "1y", "year" -> now.minusYears(1);
		case "all", "all time" -> null;
		default -> null;
		};
	}
}
