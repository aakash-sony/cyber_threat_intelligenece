package com.cyberthreat.dashboard.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cyberthreat.dashboard.dto.request.ThreatCreateRequest;
import com.cyberthreat.dashboard.dto.request.ThreatFilterRequest;
import com.cyberthreat.dashboard.dto.response.PageResponse;
import com.cyberthreat.dashboard.dto.response.SyncStatusResponse;
import com.cyberthreat.dashboard.dto.response.ThreatResponse;
import com.cyberthreat.dashboard.enums.IndicatorType;
import com.cyberthreat.dashboard.enums.Severity;
import com.cyberthreat.dashboard.enums.ThreatStatus;
import com.cyberthreat.dashboard.enums.ThreatType;
import com.cyberthreat.dashboard.service.ThreatService;
import com.cyberthreat.dashboard.service.ThreatSyncService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/threats")
@RequiredArgsConstructor
public class ThreatController {

	private final ThreatService 			threatService;
	private final ThreatSyncService 		threatSyncService;

	@GetMapping("/sync-status")
	public ResponseEntity<SyncStatusResponse> getSyncStatus() {
		return ResponseEntity.ok(threatSyncService.getSyncStatus());
	}

	@PostMapping("/sync")
	public ResponseEntity<SyncStatusResponse> triggerSync() {
		return ResponseEntity.ok(threatSyncService.triggerSync());
	}

	@GetMapping("/sources")
	public ResponseEntity<List<String>> getDistinctSources() {
		return ResponseEntity.ok(threatService.getDistinctSources());
	}

	@GetMapping
	public ResponseEntity<PageResponse<ThreatResponse>> getThreats(
			@RequestParam(required = false) final String keyword,
			@RequestParam(required = false) final Severity severity,
			@RequestParam(required = false) final ThreatType threatType,
			@RequestParam(required = false) final IndicatorType indicatorType,
			@RequestParam(required = false) final ThreatStatus status,
			@RequestParam(required = false) final String country,
			@RequestParam(required = false) final String source,
			@RequestParam(required = false) final String target,
			@RequestParam(required = false) final String timeRange,
			@RequestParam(defaultValue = "0") final int page,
			@RequestParam(defaultValue = "10") final int size,
			@RequestParam(defaultValue = "lastSeen") final String sortBy,
			@RequestParam(defaultValue = "desc") final String direction) {

		final var sortDirection 			= "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
		final var cleanSortBy 				= sanitizeSortBy(sortBy);
		final Pageable pageable 			= PageRequest.of(page, size, Sort.by(new Sort.Order(sortDirection, cleanSortBy, Sort.NullHandling.NULLS_LAST)));

		final var filter = ThreatFilterRequest.builder()
				.keyword(keyword)
				.severity(severity)
				.threatType(threatType)
				.indicatorType(indicatorType)
				.status(status)
				.country(country)
				.source(source)
				.target(target)
				.timeRange(timeRange)
				.build();

		return ResponseEntity.ok(threatService.getThreats(filter, pageable));
	}

	private String sanitizeSortBy(final String sortBy) {
		if (sortBy == null || sortBy.isBlank()) {
			return "lastSeen";
		}
		return switch (sortBy.trim().toLowerCase()) {
			case "last_seen", "lastseen" -> "lastSeen";
			case "first_seen", "firstseen" -> "firstSeen";
			case "created_at", "createdat" -> "createdAt";
			case "updated_at", "updatedat" -> "updatedAt";
			case "threat_type", "threattype" -> "threatType";
			case "indicator_type", "indicatortype" -> "indicatorType";
			case "confidence" -> "confidence";
			case "severity" -> "severity";
			case "country" -> "country";
			case "source" -> "source";
			case "indicator" -> "indicator";
			default -> "lastSeen";
		};
	}

	@GetMapping("/latest")
	public ResponseEntity<List<ThreatResponse>> getLatestThreats(
			@RequestParam(defaultValue = "10") final int limit,
			@RequestParam(defaultValue = "24h") final String timeRange,
			@RequestParam(required = false) final String country) {
		return ResponseEntity.ok(threatService.getLatestThreats(limit, timeRange, country));
	}

	@GetMapping("/critical")
	public ResponseEntity<PageResponse<ThreatResponse>> getCriticalThreats(
			@RequestParam(defaultValue = "0") final int page,
			@RequestParam(defaultValue = "10") final int size) {
		final Pageable pageable = PageRequest.of(page, size, Sort.by(new Sort.Order(Sort.Direction.DESC, "lastSeen", Sort.NullHandling.NULLS_LAST)));
		return ResponseEntity.ok(threatService.getThreatsBySeverity(Severity.CRITICAL, pageable));
	}

	@GetMapping("/high")
	public ResponseEntity<PageResponse<ThreatResponse>> getHighThreats(
			@RequestParam(defaultValue = "0") final int page,
			@RequestParam(defaultValue = "10") final int size) {
		final Pageable pageable = PageRequest.of(page, size, Sort.by(new Sort.Order(Sort.Direction.DESC, "lastSeen", Sort.NullHandling.NULLS_LAST)));
		return ResponseEntity.ok(threatService.getThreatsBySeverity(Severity.HIGH, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ThreatResponse> getThreatById(@PathVariable final Long id) {
		return ResponseEntity.ok(threatService.getThreatById(id));
	}

	@PostMapping
	public ResponseEntity<ThreatResponse> createThreat(@Valid @RequestBody final ThreatCreateRequest request) {
		final var created = threatService.createOrUpdateThreat(request);
		return new ResponseEntity<>(created, HttpStatus.CREATED);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteThreat(@PathVariable final Long id) {
		threatService.deleteThreat(id);
		return ResponseEntity.noContent().build();
	}
}
