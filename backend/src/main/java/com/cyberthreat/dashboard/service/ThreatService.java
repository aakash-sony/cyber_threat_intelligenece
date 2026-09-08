package com.cyberthreat.dashboard.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.cyberthreat.dashboard.dto.request.ThreatCreateRequest;
import com.cyberthreat.dashboard.dto.request.ThreatFilterRequest;
import com.cyberthreat.dashboard.dto.response.BatchIngestResult;
import com.cyberthreat.dashboard.dto.response.PageResponse;
import com.cyberthreat.dashboard.dto.response.ThreatResponse;
import com.cyberthreat.dashboard.enums.Severity;

public interface ThreatService {

	PageResponse<ThreatResponse> getThreats(ThreatFilterRequest filter, Pageable pageable);

	List<ThreatResponse> getLatestThreats(int limit, String timeRange, String country);

	ThreatResponse getThreatById(Long id);

	PageResponse<ThreatResponse> getThreatsBySeverity(Severity severity, Pageable pageable);

	ThreatResponse createOrUpdateThreat(ThreatCreateRequest request);

	int batchIngest(List<ThreatCreateRequest> requests);

	BatchIngestResult batchIngestDetailed(List<ThreatCreateRequest> requests);

	int ingestFromProvider(ThreatIntelligenceProvider provider);

	List<String> getDistinctSources();

	void deleteThreat(Long id);
}
