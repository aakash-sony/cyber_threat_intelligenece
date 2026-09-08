package com.cyberthreat.dashboard.service;

import com.cyberthreat.dashboard.dto.request.ThreatCreateRequest;
import com.cyberthreat.dashboard.service.impl.OpenPhishFeedProvider;
import com.cyberthreat.dashboard.service.impl.ThreatFoxFeedProvider;
import com.cyberthreat.dashboard.service.impl.UrlhausFeedProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedProviderPaginationTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("URLhaus feed provider handles multiple threat records up to batch limit")
    void testUrlhausBatchProcessing() {
        final UrlhausFeedProvider provider = new UrlhausFeedProvider(restTemplate, objectMapper);
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "batchLimit", 5);

        final String json = """
            {
              "1": [{"url": "https://bad1.com/malware", "url_status": "online", "threat": "malware_download", "reporter": "sec", "dateadded": "2026-09-01 12:00:00 UTC", "tags": ["elf"]}],
              "2": [{"url": "https://bad2.com/malware", "url_status": "offline", "threat": "malware_download", "reporter": "sec", "dateadded": "2026-09-01 13:00:00 UTC", "tags": ["mirai"]}],
              "3": [{"url": "https://bad3.com/malware", "url_status": "online", "threat": "malware_download", "reporter": "sec", "dateadded": "2026-09-01 14:00:00 UTC", "tags": ["trojan"]}]
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        final List<ThreatCreateRequest> threats = provider.fetchThreats();
        assertNotNull(threats);
        assertEquals(3, threats.size());
        assertEquals("https://bad1.com/malware", threats.get(0).getIndicator());
        // Confirm country is NOT fabricated
        assertNull(threats.get(0).getCountry());
    }

    @Test
    @DisplayName("URLhaus feed provider handles empty payload without failing")
    void testUrlhausEmptyPayload() {
        final UrlhausFeedProvider provider = new UrlhausFeedProvider(restTemplate, objectMapper);
        ReflectionTestUtils.setField(provider, "enabled", true);

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");

        final List<ThreatCreateRequest> threats = provider.fetchThreats();
        assertNotNull(threats);
        assertTrue(threats.isEmpty());
    }

    @Test
    @DisplayName("URLhaus feed provider handles API failure gracefully")
    void testUrlhausApiFailure() {
        final UrlhausFeedProvider provider = new UrlhausFeedProvider(restTemplate, objectMapper);
        ReflectionTestUtils.setField(provider, "enabled", true);

        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        final List<ThreatCreateRequest> threats = provider.fetchThreats();
        assertNotNull(threats);
        assertTrue(threats.isEmpty());
    }

    @Test
    @DisplayName("ThreatFox feed provider processes IOC batches without fabricating country")
    void testThreatFoxBatchProcessing() {
        final ThreatFoxFeedProvider provider = new ThreatFoxFeedProvider(restTemplate, objectMapper);
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "batchLimit", 10);

        final String json = """
            {
              "1001": [{"ioc_value": "198.51.100.5", "ioc_type": "ip:port", "threat_type": "botnet_cc", "malware_printable": "Cobalt Strike", "confidence_level": 90, "first_seen_utc": "2026-09-01 10:00:00"}]
            }
            """;

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

        final List<ThreatCreateRequest> threats = provider.fetchThreats();
        assertNotNull(threats);
        assertEquals(1, threats.size());
        assertEquals("198.51.100.5", threats.get(0).getIndicator());
        // Confirm country is NOT hardcoded to Global
        assertNull(threats.get(0).getCountry());
    }

    @Test
    @DisplayName("OpenPhish feed provider processes line feeds up to limit")
    void testOpenPhishBatchProcessing() {
        final OpenPhishFeedProvider provider = new OpenPhishFeedProvider(restTemplate);
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "batchLimit", 2);

        final String textFeed = "https://phish1.com/login\nhttps://phish2.com/verify\nhttps://phish3.com/account\n";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(textFeed);

        final List<ThreatCreateRequest> threats = provider.fetchThreats();
        assertNotNull(threats);
        assertEquals(2, threats.size()); // Respected batchLimit of 2
        assertEquals("https://phish1.com/login", threats.get(0).getIndicator());
        assertNull(threats.get(0).getCountry());
    }
}
