package com.cyberthreat.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedSourceResponse {
    private String name;
    private String description;
    private String url;
    private String type;
    private String status;
    private long count;
    private double percentage;
    private String color;
    private LocalDateTime lastDetected;
}
