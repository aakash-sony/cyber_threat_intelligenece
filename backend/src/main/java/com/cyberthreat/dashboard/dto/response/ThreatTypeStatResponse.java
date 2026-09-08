package com.cyberthreat.dashboard.dto.response;

import com.cyberthreat.dashboard.enums.ThreatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatTypeStatResponse {
    private ThreatType threatType;
    private long count;
    private double percentage;
}
