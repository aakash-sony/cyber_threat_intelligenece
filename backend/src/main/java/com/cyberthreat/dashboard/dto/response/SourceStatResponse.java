package com.cyberthreat.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceStatResponse {
    private String source;
    private long count;
    private double percentage;
    private String color;
}
