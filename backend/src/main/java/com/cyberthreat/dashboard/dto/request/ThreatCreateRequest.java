package com.cyberthreat.dashboard.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import com.cyberthreat.dashboard.enums.IndicatorType;
import com.cyberthreat.dashboard.enums.Severity;
import com.cyberthreat.dashboard.enums.ThreatStatus;
import com.cyberthreat.dashboard.enums.ThreatType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatCreateRequest {

	@NotBlank(message = "Indicator must not be blank")
	private String indicator;

	@NotNull(message = "Indicator type is required")
	private IndicatorType indicatorType;

	@NotNull(message = "Threat type is required")
	private ThreatType threatType;

	@NotNull(message = "Severity is required")
	private Severity severity;

	private Integer confidence;
	private String description;
	private String country;

	@NotBlank(message = "Source must not be blank")
	private String source;

	private String sourceUrl;
	private String target;
	private ThreatStatus status;
	private List<String> tags;
	private LocalDateTime firstSeen;
	private LocalDateTime lastSeen;
}
