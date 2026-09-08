package com.cyberthreat.dashboard.util;

import com.cyberthreat.dashboard.enums.IndicatorType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThreatUtilsTest {

    @Test
    void testThreatUrlUtils() {
        String urlhausLink = ThreatUrlUtils.buildExactCaseUrl("URLhaus", "https://malicious.com", IndicatorType.URL);
        assertTrue(urlhausLink.contains("urlhaus.abuse.ch"));

        String threatfoxLink = ThreatUrlUtils.buildExactCaseUrl("ThreatFox", "ioc123", IndicatorType.URL);
        assertTrue(threatfoxLink.contains("threatfox.abuse.ch"));

        String ncrpLink = ThreatUrlUtils.buildExactCaseUrl("I4C / NCRP", "fraud", IndicatorType.URL);
        assertEquals("https://cybercrime.gov.in/", ncrpLink);
    }
}
