package com.cyberthreat.dashboard;

import com.cyberthreat.dashboard.dto.response.SyncStatusResponse;
import com.cyberthreat.dashboard.service.ThreatSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ThreatSyncServiceTest {

    @Autowired
    private ThreatSyncService threatSyncService;

    @Test
    void testGetSyncStatus() {
        SyncStatusResponse status = threatSyncService.getSyncStatus();
        assertNotNull(status);
        assertNotNull(status.getStatus());
        assertNotNull(status.getProviders());
        assertFalse(status.getProviders().isEmpty());
    }
}
