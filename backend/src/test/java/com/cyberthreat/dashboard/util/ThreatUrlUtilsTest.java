package com.cyberthreat.dashboard.util;

import com.cyberthreat.dashboard.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThreatUrlUtilsTest {

    @Test
    @DisplayName("Should fall back to VirusTotal when source is null or empty")
    void testVirusTotalFallback() {
        final String url1 = ThreatUrlUtils.buildInvestigationUrl(null, "8.8.8.8", IndicatorType.IP);
        assertTrue(url1.contains("virustotal.com/gui/search/8.8.8.8"));

        final String url2 = ThreatUrlUtils.buildInvestigationUrl("", "malware.exe", IndicatorType.HASH);
        assertTrue(url2.contains("virustotal.com/gui/search/malware.exe"));

        final String url3 = ThreatUrlUtils.buildInvestigationUrl("UnknownSource", "https://bad.com", IndicatorType.URL);
        assertTrue(url3.contains("virustotal.com/gui/search/"));
    }

    @Test
    @DisplayName("Should build valid PhishTank investigation URL")
    void testPhishTankUrl() {
        final String url = ThreatUrlUtils.buildInvestigationUrl("PhishTank", "https://bank-login.com", IndicatorType.URL);
        assertTrue(url.contains("phishtank.org/phish_search.php"));
        assertTrue(url.contains("https%3A%2F%2Fbank-login.com"));

        final String empty = ThreatUrlUtils.buildPhishTankUrl(null);
        assertEquals("https://phishtank.org/", empty);
    }

    @Test
    @DisplayName("Should build valid URLhaus investigation URL")
    void testUrlHausUrl() {
        final String url = ThreatUrlUtils.buildInvestigationUrl("URLhaus", "https://malicious.com/bin", IndicatorType.URL);
        assertTrue(url.contains("urlhaus.abuse.ch/browse.php?search="));
        assertTrue(url.contains("https%3A%2F%2Fmalicious.com%2Fbin"));

        final String empty = ThreatUrlUtils.buildUrlHausUrl("   ");
        assertEquals("https://urlhaus.abuse.ch/browse/", empty);
    }

    @Test
    @DisplayName("Should build valid ThreatFox URL with IOC ID vs arbitrary search")
    void testThreatFoxUrl() {
        // IOC numeric ID or alphanumeric string
        final String iocUrl = ThreatUrlUtils.buildInvestigationUrl("ThreatFox", "12345", IndicatorType.URL);
        assertEquals("https://threatfox.abuse.ch/ioc/12345/", iocUrl);

        // Complex indicator with spaces or special chars goes to search
        final String searchUrl = ThreatUrlUtils.buildInvestigationUrl("ThreatFox", "malware payload", IndicatorType.URL);
        assertTrue(searchUrl.contains("threatfox.abuse.ch/browse.php?search=ioc%3A"));

        final String empty = ThreatUrlUtils.buildThreatFoxUrl(null);
        assertEquals("https://threatfox.abuse.ch/browse/", empty);
    }

    @Test
    @DisplayName("Should build AlienVault OTX URLs by indicator type")
    void testAlienVaultOtxUrl() {
        // IP indicator
        final String ipUrl = ThreatUrlUtils.buildInvestigationUrl("AlienVault OTX", "198.51.100.1", IndicatorType.IP);
        assertEquals("https://otx.alienvault.com/indicator/ip/198.51.100.1", ipUrl);

        // Domain indicator
        final String domUrl = ThreatUrlUtils.buildInvestigationUrl("OTX", "bad-c2.net", IndicatorType.DOMAIN);
        assertEquals("https://otx.alienvault.com/indicator/domain/bad-c2.net", domUrl);

        // Hash indicator (MD5/SHA256)
        final String hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        final String hashUrl = ThreatUrlUtils.buildInvestigationUrl("AlienVault", hash, IndicatorType.HASH);
        assertEquals("https://otx.alienvault.com/indicator/file/" + hash, hashUrl);

        // Raw URL indicator
        final String urlUrl = ThreatUrlUtils.buildInvestigationUrl("OTX", "https://bad.com/drop", IndicatorType.URL);
        assertTrue(urlUrl.contains("otx.alienvault.com/indicator/url/"));
    }

    @Test
    @DisplayName("Should build OpenPhish investigation URL")
    void testOpenPhishUrl() {
        final String url = ThreatUrlUtils.buildInvestigationUrl("OpenPhish", "https://phish.org/test", IndicatorType.URL);
        assertTrue(url.contains("openphish.com/?url="));
        assertTrue(url.contains("https%3A%2F%2Fphish.org%2Ftest"));

        final String empty = ThreatUrlUtils.buildOpenPhishUrl("");
        assertEquals("https://openphish.com/", empty);
    }

    @Test
    @DisplayName("Should resolve I4C / NCRP portal URL")
    void testCybercrimeGovIn() {
        final String ncrp = ThreatUrlUtils.buildInvestigationUrl("I4C / NCRP", "financial fraud", IndicatorType.URL);
        assertEquals("https://cybercrime.gov.in/", ncrp);

        final String cybercrime = ThreatUrlUtils.buildInvestigationUrl("cybercrime", "scam", IndicatorType.URL);
        assertEquals("https://cybercrime.gov.in/", cybercrime);
    }

    @Test
    @DisplayName("Should properly encode special characters and preserve backward compatibility")
    void testSpecialCharactersAndBackwardCompatibility() {
        final String encoded = ThreatUrlUtils.buildExactCaseUrl("URLhaus", "https://test.com/path?q=1&b=2#frag", IndicatorType.URL);
        assertTrue(encoded.contains("https%3A%2F%2Ftest.com%2Fpath%3Fq%3D1%26b%3D2%23frag"));

        // VirusTotal handles null indicator safely
        final String vtNull = ThreatUrlUtils.buildVirusTotalUrl(null);
        assertEquals("https://www.virustotal.com/gui/search/", vtNull);
    }
}
