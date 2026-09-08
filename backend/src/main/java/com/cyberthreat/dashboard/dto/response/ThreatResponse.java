package com.cyberthreat.dashboard.dto.response;

import com.cyberthreat.dashboard.enums.IndicatorType;
import com.cyberthreat.dashboard.enums.Severity;
import com.cyberthreat.dashboard.enums.ThreatStatus;
import com.cyberthreat.dashboard.enums.ThreatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatResponse {
    private Long id;
    private String indicator;
    private IndicatorType indicatorType;
    private ThreatType threatType;
    private Severity severity;
    private Integer confidence;
    private String description;
    private String country;
    private String countryCode;
    private String countryName;
    private String geoSource;
    private String source;
    private String sourceUrl;
    private String investigationUrl;
    private String target;
    private ThreatStatus status;
    private List<String> tags;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getCountryName() {
        return countryName != null && !countryName.isBlank() ? countryName : country;
    }

    public String getInvestigationUrl() {
        return investigationUrl != null && !investigationUrl.isBlank() ? investigationUrl : sourceUrl;
    }
}
