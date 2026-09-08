package com.cyberthreat.dashboard.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Expression;

import com.cyberthreat.dashboard.dto.request.ThreatCreateRequest;
import com.cyberthreat.dashboard.dto.request.ThreatFilterRequest;
import com.cyberthreat.dashboard.dto.response.BatchIngestResult;
import com.cyberthreat.dashboard.dto.response.GeoLocationDto;
import com.cyberthreat.dashboard.dto.response.PageResponse;
import com.cyberthreat.dashboard.dto.response.ThreatResponse;
import com.cyberthreat.dashboard.entity.ThreatEntity;
import com.cyberthreat.dashboard.enums.Severity;
import com.cyberthreat.dashboard.exception.ResourceNotFoundException;
import com.cyberthreat.dashboard.mapper.ThreatMapper;
import com.cyberthreat.dashboard.repository.ThreatRepository;
import com.cyberthreat.dashboard.repository.ThreatSpecification;
import com.cyberthreat.dashboard.service.GeoLocationService;
import com.cyberthreat.dashboard.service.ThreatIntelligenceProvider;
import com.cyberthreat.dashboard.service.ThreatService;
import com.cyberthreat.dashboard.util.ThreatUrlUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreatServiceImpl implements ThreatService {

	private final ThreatRepository threatRepository;
	private final ThreatMapper threatMapper;
	private final GeoLocationService geoLocationService;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ThreatResponse> getThreats(final ThreatFilterRequest filter, final Pageable pageable) {
		final var spec = ThreatSpecification.withFilter(filter);
		final var page = threatRepository.findAll(spec, pageable);
		final List<ThreatResponse> responses = page.getContent().stream()
				.map(threatMapper::toResponse)
				.collect(Collectors.toList());

		return PageResponse.<ThreatResponse>builder()
				.content(responses)
				.pageNumber(page.getNumber())
				.pageSize(page.getSize())
				.totalElements(page.getTotalElements())
				.totalPages(page.getTotalPages())
				.isFirst(page.isFirst())
				.isLast(page.isLast())
				.build();
	}

	@Override
	@Transactional(readOnly = true)
	public List<ThreatResponse> getLatestThreats(final int limit, final String timeRange, final String country) {
		final var startTime = parseStartTime(timeRange);
		final var normalizedCountry = country == null || country.isBlank() || "all".equalsIgnoreCase(country) || "all countries".equalsIgnoreCase(country) ? null : country.trim().toLowerCase();
		final var maxResults = Math.max(1, limit);
		final Pageable pageable = PageRequest.of(0, maxResults, Sort.by(new Sort.Order(Sort.Direction.DESC, "lastSeen", Sort.NullHandling.NULLS_LAST)));

		final Specification<ThreatEntity> spec = (root, query, cb) -> {
			final List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
			if (startTime != null) {
				final Expression<LocalDateTime> timestampExpr = cb.<LocalDateTime>coalesce(root.get("lastSeen"), root.get("createdAt"));
				predicates.add(cb.greaterThanOrEqualTo(timestampExpr, startTime));
			}
			if (normalizedCountry != null)
				predicates.add(cb.equal(cb.lower(root.get("country")), normalizedCountry));
			return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
		};

		final var list = threatRepository.findAll(spec, pageable).getContent();
		return list.stream().map(threatMapper::toResponse).collect(Collectors.toList());
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

	@Override
	@Transactional(readOnly = true)
	public ThreatResponse getThreatById(final Long id) {
		final var entity = threatRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Threat not found with ID: " + id));
		return threatMapper.toResponse(entity);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ThreatResponse> getThreatsBySeverity(final Severity severity, final Pageable pageable) {
		final var page = threatRepository.findBySeverity(severity, pageable);
		final List<ThreatResponse> responses = page.getContent().stream()
				.map(threatMapper::toResponse)
				.collect(Collectors.toList());

		return PageResponse.<ThreatResponse>builder()
				.content(responses)
				.pageNumber(page.getNumber())
				.pageSize(page.getSize())
				.totalElements(page.getTotalElements())
				.totalPages(page.getTotalPages())
				.isFirst(page.isFirst())
				.isLast(page.isLast())
				.build();
	}

	@Override
	@Transactional
	public ThreatResponse createOrUpdateThreat(final ThreatCreateRequest request) {
		final String rawIndicator = request.getIndicator().trim();
		final String cleanIndicator = rawIndicator.length() > 500 ? rawIndicator.substring(0, 500) : rawIndicator;
		request.setIndicator(cleanIndicator);

		// Prevent duplicates based on unique combination of indicator, indicatorType, and source
		final var existingOpt = threatRepository.findByIndicatorAndIndicatorTypeAndSource(
				cleanIndicator, request.getIndicatorType(), request.getSource().trim()
		);

		// Resolve authentic IP geolocation without fabrication
		var resolvedCountry = request.getCountry();
		String resolvedCountryCode = null;
		String resolvedGeoSource = null;

		if (resolvedCountry == null || resolvedCountry.isBlank() || "Global".equalsIgnoreCase(resolvedCountry) || "Unknown".equalsIgnoreCase(resolvedCountry)) {
			try {
				final var geoDto = geoLocationService.resolveIndicator(request.getIndicator(), request.getIndicatorType());
				if (geoDto != null) {
					resolvedCountry = geoDto.getCountryName();
					resolvedCountryCode = geoDto.getCountryCode();
					resolvedGeoSource = geoDto.getGeoSource();
				}
			} catch (final Exception e) {
				log.warn("GeoLocationService error for {}: {}", request.getIndicator(), e.getMessage());
				resolvedCountry = "Unknown";
			}
		}

		if (resolvedCountry == null || resolvedCountry.isBlank()) {
			resolvedCountry = "Unknown";
		}

		// Resolve specific source investigation URL
		var resolvedUrl = request.getSourceUrl();
		if (resolvedUrl == null || isGenericUrl(resolvedUrl)) {
			resolvedUrl = ThreatUrlUtils.buildInvestigationUrl(
					request.getSource(), request.getIndicator(), request.getIndicatorType()
			);
		}

		if (resolvedCountry != null && resolvedCountry.length() > 100)
			resolvedCountry = resolvedCountry.substring(0, 100);
		if (resolvedCountryCode != null && resolvedCountryCode.length() > 10)
			resolvedCountryCode = resolvedCountryCode.substring(0, 10);
		if (resolvedGeoSource != null && resolvedGeoSource.length() > 100)
			resolvedGeoSource = resolvedGeoSource.substring(0, 100);
		if (resolvedUrl != null && resolvedUrl.length() > 1000)
			resolvedUrl = resolvedUrl.substring(0, 1000);

		ThreatEntity entity;
		if (existingOpt.isPresent()) {
			entity = existingOpt.get();
			// Preserve firstSeen, update lastSeen ONLY if newer
			if (request.getLastSeen() != null) {
				if (entity.getLastSeen() == null || request.getLastSeen().isAfter(entity.getLastSeen())) {
					entity.setLastSeen(request.getLastSeen());
				}
			}
			entity.setConfidence(request.getConfidence() != null ? request.getConfidence() : entity.getConfidence());
			entity.setSeverity(request.getSeverity() != null ? request.getSeverity() : entity.getSeverity());
			entity.setDescription(request.getDescription() != null ? request.getDescription() : entity.getDescription());
			entity.setStatus(request.getStatus() != null ? request.getStatus() : entity.getStatus());
			if (request.getTarget() != null) {
				final var tgt = request.getTarget();
				entity.setTarget(tgt.length() > 100 ? tgt.substring(0, 100) : tgt);
			}
			entity.setSourceUrl(resolvedUrl);
			entity.setCountry(resolvedCountry);
			if (resolvedCountryCode != null) entity.setCountryCode(resolvedCountryCode);
			if (resolvedGeoSource != null) entity.setGeoSource(resolvedGeoSource);
			if (request.getTags() != null && !request.getTags().isEmpty()) {
				final var tagStr = String.join(",", request.getTags());
				entity.setTags(tagStr.length() > 500 ? tagStr.substring(0, 500) : tagStr);
			}
			log.debug("Updated existing threat: {} [{}]", entity.getIndicator(), entity.getId());
		} else {
			entity = threatMapper.toEntity(request);
			if (request.getTarget() != null && request.getTarget().length() > 100) {
				entity.setTarget(request.getTarget().substring(0, 100));
			}
			entity.setSourceUrl(resolvedUrl);
			entity.setCountry(resolvedCountry);
			entity.setCountryCode(resolvedCountryCode);
			entity.setGeoSource(resolvedGeoSource);
			if (request.getTags() != null && !request.getTags().isEmpty()) {
				final var tagStr = String.join(",", request.getTags());
				entity.setTags(tagStr.length() > 500 ? tagStr.substring(0, 500) : tagStr);
			}
			log.debug("Ingesting new threat: {} [{}]", entity.getIndicator(), entity.getSeverity());
		}

		final var saved = threatRepository.save(entity);
		return threatMapper.toResponse(saved);
	}

	private boolean isGenericUrl(final String url) {
		if (url == null || url.isBlank()) return true;
		final var clean = url.trim().replaceAll("/+$", "");
		return "https://urlhaus.abuse.ch".equals(clean) ||
				"https://urlhaus.abuse.ch/browse".equals(clean) ||
				"https://openphish.com".equals(clean) ||
				"https://threatfox.abuse.ch".equals(clean) ||
				"https://tweetfeed.live".equals(clean);
	}

	@Override
	@Transactional
	public int batchIngest(final List<ThreatCreateRequest> requests) {
		final var result = batchIngestDetailed(requests);
		return result.getInserted() + result.getUpdated();
	}

	@Override
	@Transactional
	public BatchIngestResult batchIngestDetailed(final List<ThreatCreateRequest> requests) {
		if (requests == null || requests.isEmpty()) {
			return BatchIngestResult.empty();
		}

		final long startNs = System.currentTimeMillis();
		int skippedCount = 0;
		int failedCount = 0;

		// 1. Deduplicate incoming batch by unique key (indicator + indicatorType + source)
		final Map<String, ThreatCreateRequest> deduplicated = new LinkedHashMap<>();
		for (final ThreatCreateRequest req : requests) {
			if (req == null || req.getIndicator() == null || req.getIndicator().isBlank() || req.getSource() == null || req.getSource().isBlank()) {
				skippedCount++;
				continue;
			}
			final var trimmedInd = req.getIndicator().trim();
			if (trimmedInd.length() > 500) {
				req.setIndicator(trimmedInd.substring(0, 500));
			} else {
				req.setIndicator(trimmedInd);
			}
			final var key = buildUniqueKey(req.getIndicator(), req.getIndicatorType().name(), req.getSource());
			deduplicated.put(key, req);
		}

		if (deduplicated.isEmpty()) {
			return BatchIngestResult.builder()
					.fetched(requests.size())
					.inserted(0)
					.updated(0)
					.skipped(skippedCount)
					.failed(0)
					.durationMs(System.currentTimeMillis() - startNs)
					.build();
		}

		// Group deduplicated indicators by source to query existing records efficiently
		final Map<String, List<String>> indicatorsBySource = new HashMap<>();
		for (final ThreatCreateRequest req : deduplicated.values()) {
			indicatorsBySource.computeIfAbsent(req.getSource().trim(), k -> new ArrayList<>()).add(req.getIndicator().trim());
		}

		// 2. Query only existing entities matching the incoming indicators in chunks of 500 (scalable memory usage)
		final Map<String, ThreatEntity> existingMap = new HashMap<>();
		for (final Map.Entry<String, List<String>> entry : indicatorsBySource.entrySet()) {
			final String source = entry.getKey();
			final List<String> indicators = entry.getValue();
			final int chunkSize = 500;
			for (int i = 0; i < indicators.size(); i += chunkSize) {
				final List<String> chunk = indicators.subList(i, Math.min(i + chunkSize, indicators.size()));
				try {
					final List<ThreatEntity> found = threatRepository.findBySourceAndIndicatorIn(source, chunk);
					for (final ThreatEntity e : found) {
						if (e.getIndicator() != null && e.getIndicatorType() != null && e.getSource() != null) {
							final var key = buildUniqueKey(e.getIndicator(), e.getIndicatorType().name(), e.getSource());
							existingMap.put(key, e);
						}
					}
				} catch (final Exception e) {
					log.error("Error querying existing indicators for source {}: {}", source, e.getMessage());
				}
			}
		}

		// 3. Batch resolve IP geolocation for all unique indicators
		final List<String> allIndicators = deduplicated.values().stream()
				.map(ThreatCreateRequest::getIndicator)
				.filter(Objects::nonNull)
				.toList();
		Map<String, GeoLocationDto> geoMap = Collections.emptyMap();
		try {
			geoMap = geoLocationService.resolveIndicatorsBatch(allIndicators);
		} catch (final Exception e) {
			log.warn("Batch geolocation lookup failed: {}", e.getMessage());
		}

		// 4. Process entities: Insert vs Update
		int insertedCount = 0;
		int updatedCount = 0;
		final List<ThreatEntity> toSave = new ArrayList<>(deduplicated.size());

		for (final ThreatCreateRequest req : deduplicated.values()) {
			try {
				final var key = buildUniqueKey(req.getIndicator(), req.getIndicatorType().name(), req.getSource());
				final var existing = existingMap.get(key);

				var resolvedCountry = req.getCountry();
				String resolvedCountryCode = null;
				String resolvedGeoSource = null;

				if (resolvedCountry == null || resolvedCountry.isBlank() || "Global".equalsIgnoreCase(resolvedCountry) || "Unknown".equalsIgnoreCase(resolvedCountry)) {
					final var geoDto = geoMap.get(req.getIndicator());
					if (geoDto != null && geoDto.getCountryName() != null && !geoDto.getCountryName().isBlank()) {
						resolvedCountry = geoDto.getCountryName();
						resolvedCountryCode = geoDto.getCountryCode();
						resolvedGeoSource = geoDto.getGeoSource();
					} else {
						resolvedCountry = "Unknown";
					}
				}

				if (resolvedCountry == null || resolvedCountry.isBlank()) {
					resolvedCountry = "Unknown";
				}

				var resolvedUrl = req.getSourceUrl();
				if (resolvedUrl == null || isGenericUrl(resolvedUrl)) {
					resolvedUrl = ThreatUrlUtils.buildInvestigationUrl(req.getSource(), req.getIndicator(), req.getIndicatorType());
				}

				if (resolvedCountry != null && resolvedCountry.length() > 100) resolvedCountry = resolvedCountry.substring(0, 100);
				if (resolvedCountryCode != null && resolvedCountryCode.length() > 10) resolvedCountryCode = resolvedCountryCode.substring(0, 10);
				if (resolvedGeoSource != null && resolvedGeoSource.length() > 100) resolvedGeoSource = resolvedGeoSource.substring(0, 100);
				if (resolvedUrl != null && resolvedUrl.length() > 1000) resolvedUrl = resolvedUrl.substring(0, 1000);

				final ThreatEntity entity;
				if (existing != null) {
					// UPDATE existing record: PRESERVE firstSeen, update lastSeen only if newer observation exists
					entity = existing;
					if (req.getLastSeen() != null) {
						if (entity.getLastSeen() == null || req.getLastSeen().isAfter(entity.getLastSeen())) {
							entity.setLastSeen(req.getLastSeen());
						}
					}
					entity.setConfidence(req.getConfidence() != null ? req.getConfidence() : entity.getConfidence());
					entity.setSeverity(req.getSeverity() != null ? req.getSeverity() : entity.getSeverity());
					entity.setDescription(req.getDescription() != null ? req.getDescription() : entity.getDescription());
					entity.setStatus(req.getStatus() != null ? req.getStatus() : entity.getStatus());
					if (req.getTarget() != null) {
						final var tgt = req.getTarget();
						entity.setTarget(tgt.length() > 100 ? tgt.substring(0, 100) : tgt);
					}
					entity.setSourceUrl(resolvedUrl);
					entity.setCountry(resolvedCountry);
					if (resolvedCountryCode != null) entity.setCountryCode(resolvedCountryCode);
					if (resolvedGeoSource != null) entity.setGeoSource(resolvedGeoSource);
					if (req.getTags() != null && !req.getTags().isEmpty()) {
						final var tagStr = String.join(",", req.getTags());
						entity.setTags(tagStr.length() > 500 ? tagStr.substring(0, 500) : tagStr);
					}
					updatedCount++;
				} else {
					// INSERT new record
					entity = threatMapper.toEntity(req);
					if (req.getTarget() != null && req.getTarget().length() > 100) {
						entity.setTarget(req.getTarget().substring(0, 100));
					}
					entity.setSourceUrl(resolvedUrl);
					entity.setCountry(resolvedCountry);
					entity.setCountryCode(resolvedCountryCode);
					entity.setGeoSource(resolvedGeoSource);
					if (req.getTags() != null && !req.getTags().isEmpty()) {
						final var tagStr = String.join(",", req.getTags());
						entity.setTags(tagStr.length() > 500 ? tagStr.substring(0, 500) : tagStr);
					}
					insertedCount++;
				}
				toSave.add(entity);
			} catch (final Exception ex) {
				log.warn("Failed processing threat item {}: {}", req.getIndicator(), ex.getMessage());
				failedCount++;
			}
		}

		// 5. Chunked save to PostgreSQL
		final int chunkSize = 500;
		for (int i = 0; i < toSave.size(); i += chunkSize) {
			final var chunk = toSave.subList(i, Math.min(i + chunkSize, toSave.size()));
			threatRepository.saveAll(chunk);
			threatRepository.flush();
		}

		final long durationMs = System.currentTimeMillis() - startNs;
		return BatchIngestResult.builder()
				.fetched(requests.size())
				.inserted(insertedCount)
				.updated(updatedCount)
				.skipped(skippedCount)
				.failed(failedCount)
				.durationMs(durationMs)
				.build();
	}

	private String buildUniqueKey(final String indicator, final String indicatorType, final String source) {
		return indicator.trim().toLowerCase() + "#" + indicatorType.trim().toUpperCase() + "#" + source.trim().toLowerCase();
	}

	@Override
	@Transactional
	public int ingestFromProvider(final ThreatIntelligenceProvider provider) {
		log.info("Starting ingestion from provider: {}", provider.getProviderName());
		final var threats = provider.fetchThreats();
		final var result = batchIngestDetailed(threats);
		log.info("Finished ingestion from {}: fetched={}, inserted={}, updated={}, skipped={}, failed={}, duration={}ms",
				provider.getProviderName(), result.getFetched(), result.getInserted(), result.getUpdated(),
				result.getSkipped(), result.getFailed(), result.getDurationMs());
		return result.getInserted() + result.getUpdated();
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> getDistinctSources() {
		return threatRepository.findDistinctSources();
	}

	@Override
	@Transactional
	public void deleteThreat(final Long id) {
		if (!threatRepository.existsById(id))
			throw new ResourceNotFoundException("Threat not found with ID: " + id);
		threatRepository.deleteById(id);
	}
}
