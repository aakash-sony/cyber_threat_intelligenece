package com.cyberthreat.dashboard.service;

import com.cyberthreat.dashboard.dto.response.GeoLocationDto;
import com.cyberthreat.dashboard.entity.IpGeolocationEntity;
import com.cyberthreat.dashboard.enums.IndicatorType;
import com.cyberthreat.dashboard.repository.IpGeolocationRepository;
import com.cyberthreat.dashboard.service.impl.IpGeoLocationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeoLocationServiceTest {

    @Mock
    private IpGeolocationRepository ipGeolocationRepository;

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private IpGeoLocationServiceImpl geoLocationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        geoLocationService = new IpGeoLocationServiceImpl(ipGeolocationRepository, restTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should resolve and cache valid IPv4 address from public API")
    void testValidIpv4ResolvesAndCaches() {
        final String ip = "93.184.216.34";
        when(ipGeolocationRepository.findByIp(ip)).thenReturn(Optional.empty());

        final String jsonResponse = """
            {
              "ip": "93.184.216.34",
              "country_code": "US",
              "country_name": "United States",
              "region": "California",
              "city": "Los Angeles",
              "latitude": 34.0522,
              "longitude": -118.2437,
              "asn": "AS15133",
              "org": "MCI Communications Services",
              "timezone": "America/Los_Angeles"
            }
            """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));

        when(ipGeolocationRepository.save(any(IpGeolocationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        final GeoLocationDto dto = geoLocationService.resolveIp(ip);

        assertNotNull(dto);
        assertEquals("United States", dto.getCountryName());
        assertEquals("US", dto.getCountryCode());
        assertEquals("Los Angeles", dto.getCity());
        assertEquals("AS15133", dto.getAsn());
        assertTrue(dto.getGeoSource().contains("IP Geolocation"));

        verify(ipGeolocationRepository, times(1)).save(any(IpGeolocationEntity.class));
    }

    @Test
    @DisplayName("Should return cached IP immediately without calling external API")
    void testCachedIpLookup() {
        final String ip = "1.1.1.1";
        final IpGeolocationEntity cachedEntity = IpGeolocationEntity.builder()
                .ip(ip)
                .countryCode("AU")
                .countryName("Australia")
                .source("IP Geolocation (Cached)")
                .lastUpdated(LocalDateTime.now())
                .build();

        when(ipGeolocationRepository.findByIp(ip)).thenReturn(Optional.of(cachedEntity));

        final GeoLocationDto dto = geoLocationService.resolveIp(ip);

        assertNotNull(dto);
        assertEquals("Australia", dto.getCountryName());
        assertEquals("AU", dto.getCountryCode());

        // External API must NOT be called on cache hit
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Should return Unknown for invalid IPv4 format without calling external API")
    void testInvalidIpv4ReturnsUnknown() {
        final GeoLocationDto dto = geoLocationService.resolveIp("999.999.999.999");
        assertNotNull(dto);
        assertEquals("Unknown", dto.getCountryName());
        assertNull(dto.getCountryCode());

        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Should return Unknown for null or blank IP")
    void testNullOrBlankIp() {
        assertEquals("Unknown", geoLocationService.resolveIp(null).getCountryName());
        assertEquals("Unknown", geoLocationService.resolveIp("   ").getCountryName());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Should return Unknown for private/bogon IPs without making external calls")
    void testPrivateIps() {
        assertEquals("Unknown", geoLocationService.resolveIp("127.0.0.1").getCountryName());
        assertEquals("Unknown", geoLocationService.resolveIp("192.168.1.1").getCountryName());
        assertEquals("Unknown", geoLocationService.resolveIp("10.0.0.5").getCountryName());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Should handle external API timeout gracefully and return Unknown without throwing")
    void testApiTimeoutHandledGracefully() {
        final String ip = "198.51.100.20";
        when(ipGeolocationRepository.findByIp(ip)).thenReturn(Optional.empty());

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out after 5000ms"));

        final GeoLocationDto dto = geoLocationService.resolveIp(ip);

        assertNotNull(dto);
        assertEquals("Unknown", dto.getCountryName());
        assertTrue(dto.getGeoSource().contains("Exception"));
    }

    @Test
    @DisplayName("Should handle provider error responses (e.g. rate limit) gracefully")
    void testProviderErrorResponse() {
        final String ip = "198.51.100.30";
        when(ipGeolocationRepository.findByIp(ip)).thenReturn(Optional.empty());

        final String errorJson = "{\"error\": true, \"reason\": \"Rate limit exceeded\"}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(errorJson, HttpStatus.OK));

        final GeoLocationDto dto = geoLocationService.resolveIp(ip);

        assertNotNull(dto);
        assertEquals("Unknown", dto.getCountryName());
        assertTrue(dto.getGeoSource().contains("Rate limit"));
    }

    @Test
    @DisplayName("Should return Unknown for hash indicator without attempting IP resolution")
    void testHashIndicatorReturnsUnknown() {
        final String hash = "d41d8cd98f00b204e9800998ecf8427e";
        final GeoLocationDto dto = geoLocationService.resolveIndicator(hash, IndicatorType.HASH);
        assertNotNull(dto);
        assertEquals("Unknown", dto.getCountryName());
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Should batch resolve indicators and deduplicate IPs")
    void testBatchResolutionDeduplication() {
        final List<String> indicators = List.of(
                "8.8.8.8",
                "https://8.8.8.8/malware",
                "8.8.8.8"
        );

        final IpGeolocationEntity entity = IpGeolocationEntity.builder()
                .ip("8.8.8.8")
                .countryCode("US")
                .countryName("United States")
                .source("Cache")
                .lastUpdated(LocalDateTime.now())
                .build();

        when(ipGeolocationRepository.findByIpIn(anyCollection())).thenReturn(List.of(entity));

        final Map<String, GeoLocationDto> results = geoLocationService.resolveIndicatorsBatch(indicators);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("United States", results.get("8.8.8.8").getCountryName());
        assertEquals("United States", results.get("https://8.8.8.8/malware").getCountryName());

        // Zero external calls because batch cache hit succeeded
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(String.class));
    }
}
