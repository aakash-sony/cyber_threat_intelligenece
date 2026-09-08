package com.cyberthreat.dashboard.dto.request;

import com.cyberthreat.dashboard.enums.IndicatorType;
import com.cyberthreat.dashboard.enums.Severity;
import com.cyberthreat.dashboard.enums.ThreatStatus;
import com.cyberthreat.dashboard.enums.ThreatType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatFilterRequest {
	private String keyword;
	private Severity severity;
	private ThreatType threatType;
	private IndicatorType indicatorType;
	private ThreatStatus status;
	private String country;
	private String source;
	private String target;
	private String timeRange;
	private java.time.LocalDateTime since;
}
