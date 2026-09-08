package com.cyberthreat.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long totalThreats;
    private long criticalThreats;
    private long highThreats;
    private long mediumThreats;
    private long lowThreats;
    private long activeThreats;
    
    // Type-specific counts for header cards matching reference image
    private long phishingCount;
    private long malwareCount;
    private long fraudCount;

    // Indicator type specific counts (Section 18 response format)
    private long maliciousUrls;
    private long maliciousIps;
    private long domains;
    private long hashes;
    
    // Increments (e.g., today)
    private long todayTotalIncrease;
    private long todayPhishingIncrease;
    private long todayMalwareIncrease;
    private long todayFraudIncrease;
}
