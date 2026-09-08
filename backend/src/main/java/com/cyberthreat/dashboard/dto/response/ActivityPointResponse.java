package com.cyberthreat.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityPointResponse {
    private String time;
    private long phishing;
    private long malware;
    private long fraud;
    private long total;
}
