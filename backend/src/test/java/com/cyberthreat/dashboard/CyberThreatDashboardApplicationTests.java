package com.cyberthreat.dashboard;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.cyberthreat.dashboard.repository.ThreatRepository;

@SpringBootTest
class CyberThreatDashboardApplicationTests {

    @Autowired
    private ThreatRepository threatRepository;

    @Test
    void testDashboardSummaryMetricsNonNullCountry() {
        threatRepository.getDashboardSummaryMetrics("US", LocalDateTime.now().minusHours(24));
    }

    @Test
    void testDashboardSummaryMetricsNullCountryAndNullStartTime() {
        threatRepository.getDashboardSummaryMetrics(null, null);
    }

    @Test
    void testTodayIncrementsNullCountry() {
        threatRepository.getTodayIncrements(null, LocalDateTime.now().minusDays(1));
    }
}
