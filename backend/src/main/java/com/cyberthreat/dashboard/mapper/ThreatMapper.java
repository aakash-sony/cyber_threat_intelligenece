package com.cyberthreat.dashboard.mapper;

import com.cyberthreat.dashboard.dto.request.ThreatCreateRequest;
import com.cyberthreat.dashboard.dto.response.ThreatResponse;
import com.cyberthreat.dashboard.entity.ThreatEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ThreatMapper {

    public ThreatResponse toResponse(ThreatEntity entity) {
        if (entity == null) {
            return null;
        }

        List<String> tagList = Collections.emptyList();
        if (entity.getTags() != null && !entity.getTags().trim().isEmpty()) {
            tagList = Arrays.stream(entity.getTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return ThreatResponse.builder()
                .id(entity.getId())
                .indicator(entity.getIndicator())
                .indicatorType(entity.getIndicatorType())
                .threatType(entity.getThreatType())
                .severity(entity.getSeverity())
                .confidence(entity.getConfidence())
                .description(entity.getDescription())
                .country(entity.getCountry())
                .countryName(entity.getCountry())
                .countryCode(entity.getCountryCode())
                .geoSource(entity.getGeoSource())
                .source(entity.getSource())
                .sourceUrl(entity.getSourceUrl())
                .investigationUrl(entity.getSourceUrl())
                .target(entity.getTarget())
                .status(entity.getStatus())
                .tags(tagList)
                .firstSeen(entity.getFirstSeen())
                .lastSeen(entity.getLastSeen())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ThreatEntity toEntity(ThreatCreateRequest request) {
        if (request == null) {
            return null;
        }

        String tagsStr = null;
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            tagsStr = String.join(",", request.getTags());
        }

        return ThreatEntity.builder()
                .indicator(request.getIndicator().trim())
                .indicatorType(request.getIndicatorType())
                .threatType(request.getThreatType())
                .severity(request.getSeverity())
                .confidence(request.getConfidence() != null ? request.getConfidence() : 80)
                .description(request.getDescription())
                .country(request.getCountry())
                .source(request.getSource().trim())
                .sourceUrl(request.getSourceUrl())
                .target(request.getTarget())
                .status(request.getStatus())
                .tags(tagsStr)
                .firstSeen(request.getFirstSeen())
                .lastSeen(request.getLastSeen())
                .build();
    }
}
