package com.cyberthreat.dashboard.dto.response;

import com.cyberthreat.dashboard.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeverityStatResponse {
    private Severity severity;
    private long count;
    private double percentage;
}
