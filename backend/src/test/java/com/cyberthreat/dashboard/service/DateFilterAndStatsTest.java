package com.cyberthreat.dashboard.service;

import com.cyberthreat.dashboard.dto.response.DashboardSummaryResponse;
import com.cyberthreat.dashboard.entity.ThreatEntity;
import com.cyberthreat.dashboard.repository.ThreatRepository;
import com.cyberthreat.dashboard.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateFilterAndStatsTest {

    @Mock
    private ThreatRepository threatRepository;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(threatRepository, Collections.emptyList());
    }

    @Test
    @DisplayName("Should query 1 Year (365 days) when timeRange is 1y or year")
    void testOneYearFilter() {
        when(threatRepository.getDashboardSummaryMetrics(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{150L, 20L, 30L, 40L, 60L, 100L, 50L, 50L, 50L, 50L, 40L, 30L, 30L}));
        when(threatRepository.getTodayIncrements(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{10L, 5L, 3L, 2L}));

        final DashboardSummaryResponse summary = dashboardService.getSummary("1y", null);
        assertNotNull(summary);
        assertEquals(150L, summary.getTotalThreats());

        final ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        final List<Object[]> mockCountryRows = new java.util.ArrayList<>();
        mockCountryRows.add(new Object[]{"India", 50L});
        when(threatRepository.countGroupedByCountrySince(dateCaptor.capture()))
                .thenReturn(mockCountryRows);

        final List<Map<String, Object>> countryStats = dashboardService.getCountryStats("1y");
        assertNotNull(countryStats);
        assertEquals(1, countryStats.size());

        final LocalDateTime capturedTime = dateCaptor.getValue();
        assertNotNull(capturedTime);
        assertTrue(capturedTime.isBefore(LocalDateTime.now().minusDays(360)));
        assertTrue(capturedTime.isAfter(LocalDateTime.now().minusDays(370)));

        verify(threatRepository, never()).countGroupedByCountry();
    }

    @Test
    @DisplayName("Should query 3 Months (90 days) when timeRange is 90d, 3m, or default")
    void testThreeMonthsFilter() {
        when(threatRepository.getDashboardSummaryMetrics(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{42L, 10L, 10L, 10L, 12L, 30L, 15L, 15L, 12L, 10L, 10L, 10L, 12L}));
        when(threatRepository.getTodayIncrements(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{5L, 2L, 2L, 1L}));

        final DashboardSummaryResponse summary = dashboardService.getSummary("90d", null);
        assertNotNull(summary);
        assertEquals(42L, summary.getTotalThreats());

        // For country stats with 90d, repository countGroupedByCountrySince must be called with a non-null startTime
        final ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        final List<Object[]> mockCountryRows = new java.util.ArrayList<>();
        mockCountryRows.add(new Object[]{"India", 15L});
        when(threatRepository.countGroupedByCountrySince(dateCaptor.capture()))
                .thenReturn(mockCountryRows);

        final List<Map<String, Object>> countryStats = dashboardService.getCountryStats("90d");
        assertNotNull(countryStats);
        assertEquals(1, countryStats.size());
        assertEquals("India", countryStats.get(0).get("country"));

        // Verify startTime is approximately 90 days ago
        final LocalDateTime capturedTime = dateCaptor.getValue();
        assertNotNull(capturedTime);
        assertTrue(capturedTime.isBefore(LocalDateTime.now().minusDays(89)));
        assertTrue(capturedTime.isAfter(LocalDateTime.now().minusDays(91)));

        verify(threatRepository, never()).countGroupedByCountry();
    }

    @Test
    @DisplayName("Should query All stored historical data when timeRange is 'all'")
    void testAllTimeFilter() {
        when(threatRepository.getDashboardSummaryMetrics(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{500L, 100L, 100L, 100L, 200L, 300L, 150L, 200L, 150L, 150L, 150L, 100L, 100L}));
        when(threatRepository.getTodayIncrements(any(), any()))
                .thenReturn(Collections.singletonList(new Object[]{20L, 10L, 5L, 5L}));

        final DashboardSummaryResponse summary = dashboardService.getSummary("all", null);
        assertNotNull(summary);
        assertEquals(500L, summary.getTotalThreats());

        // For 'all', countGroupedByCountry (without date restriction) must be called
        final List<Object[]> mockAllRows = new java.util.ArrayList<>();
        mockAllRows.add(new Object[]{"United States", 200L});
        mockAllRows.add(new Object[]{"Germany", 100L});
        when(threatRepository.countGroupedByCountry())
                .thenReturn(mockAllRows);

        final List<Map<String, Object>> countryStats = dashboardService.getCountryStats("all");
        assertNotNull(countryStats);
        assertEquals(2, countryStats.size());

        verify(threatRepository, times(1)).countGroupedByCountry();
        verify(threatRepository, never()).countGroupedByCountrySince(any());
    }

    @Test
    @DisplayName("Should handle empty database gracefully with zero counts and no NPE")
    void testEmptyDatabase() {
        when(threatRepository.getDashboardSummaryMetrics(any(), any()))
                .thenReturn(Collections.emptyList());
        when(threatRepository.getTodayIncrements(any(), any()))
                .thenReturn(Collections.emptyList());
        when(threatRepository.countGroupedByCountry()).thenReturn(Collections.emptyList());

        final DashboardSummaryResponse summary = dashboardService.getSummary("all", null);
        assertNotNull(summary);
        assertEquals(0L, summary.getTotalThreats());
        assertEquals(0L, summary.getCriticalThreats());
        assertEquals(0L, summary.getMaliciousUrls());
        assertEquals(0L, summary.getMaliciousIps());

        final List<Map<String, Object>> stats = dashboardService.getCountryStats("all");
        assertNotNull(stats);
        assertTrue(stats.isEmpty());

        when(threatRepository.findMinLastSeen()).thenReturn(null);
        when(threatRepository.findMinCreatedAt()).thenReturn(null);
        final var timeline = dashboardService.getActivityTimeline("all", null);
        assertNotNull(timeline);
        assertTrue(timeline.isEmpty());
    }

    @Test
    @DisplayName("Should use min createdAt when records have no lastSeen timestamp")
    void testTimelineWithNullLastSeenFallsBackToCreatedAt() {
        when(threatRepository.findMinLastSeen()).thenReturn(null);
        when(threatRepository.findMinCreatedAt()).thenReturn(LocalDateTime.now().minusDays(2));
        when(threatRepository.count(any(Specification.class))).thenReturn(5L);

        final var timeline = dashboardService.getActivityTimeline("all", null);
        assertNotNull(timeline);
        assertFalse(timeline.isEmpty());
        verify(threatRepository, times(1)).findMinLastSeen();
        verify(threatRepository, times(1)).findMinCreatedAt();
    }
}
