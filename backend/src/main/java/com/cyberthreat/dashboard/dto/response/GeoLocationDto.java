package com.cyberthreat.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object carrying authentic IP geolocation information.
 * Notice: This represents infrastructure location, not physical attacker location.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationDto {
    private String ip;
    private String countryCode;
    private String countryName;
    private String region;
    private String city;
    private Double latitude;
    private Double longitude;
    private String asn;
    private String organization;
    private String timezone;
    private String geoSource;

    public static GeoLocationDto unknown(final String ip, final String source) {
        String safeSource = source != null ? source : "Unresolved";
        if (safeSource.length() > 80) {
            safeSource = safeSource.substring(0, 80);
        }
        return GeoLocationDto.builder()
                .ip(ip)
                .countryCode(null)
                .countryName("Unknown")
                .geoSource(safeSource)
                .build();
    }
}
