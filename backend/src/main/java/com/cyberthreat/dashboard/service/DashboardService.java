package com.cyberthreat.dashboard.service;

import java.util.List;
import java.util.Map;

import com.cyberthreat.dashboard.dto.response.ActivityPointResponse;
import com.cyberthreat.dashboard.dto.response.DashboardSummaryResponse;
import com.cyberthreat.dashboard.dto.response.DetailedSourceResponse;
import com.cyberthreat.dashboard.dto.response.SeverityStatResponse;
import com.cyberthreat.dashboard.dto.response.SourceStatResponse;
import com.cyberthreat.dashboard.dto.response.ThreatTypeStatResponse;

public interface DashboardService {

	DashboardSummaryResponse getSummary(String timeRange, String country);

	List<ActivityPointResponse> getActivityTimeline(String timeRange, String country);

	List<SourceStatResponse> getThreatsBySource(String timeRange, String country);

	List<SeverityStatResponse> getThreatsBySeverity(String timeRange, String country);

	List<ThreatTypeStatResponse> getThreatsByType(String timeRange, String country);

	List<DetailedSourceResponse> getDetailedSources();

	List<String> getAvailableCountries();

	List<Map<String, Object>> getCountryStats(String timeRange);
}
