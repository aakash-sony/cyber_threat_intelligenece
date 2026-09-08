package com.cyberthreat.dashboard.service;

import com.cyberthreat.dashboard.dto.response.SyncStatusResponse;

public interface ThreatSyncService {
	/**
	 * Get current synchronization status, timestamps, and active provider health
	 */
	SyncStatusResponse getSyncStatus();

	/**
	 * Execute live intelligence synchronization from all active providers into PostgreSQL
	 */
	SyncStatusResponse triggerSync();

	/**
	 * Execute synchronization distinguishing initial application boot from subsequent 15-minute polling
	 */
	SyncStatusResponse triggerSync(boolean isInitialSync);
}
