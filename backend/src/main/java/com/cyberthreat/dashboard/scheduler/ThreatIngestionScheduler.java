package com.cyberthreat.dashboard.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cyberthreat.dashboard.service.ThreatSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThreatIngestionScheduler {

	private final ThreatSyncService threatSyncService;

	@Value("${threat.scheduler.enabled:true}")
	private boolean schedulerEnabled;

	private final AtomicBoolean initialSyncDone = new AtomicBoolean(false);

	/**
	 * Runs automatically every 15 minutes (default 900,000 milliseconds).
	 * Initial delay triggers live ingestion 5 seconds after application boot.
	 */
	@Scheduled(fixedRateString = "${threat.scheduler.fixed-rate-ms:900000}", initialDelay = 5000)
	public void runScheduledThreatIngestion() {
		if (!schedulerEnabled) {
			log.info("Threat intelligence scheduler is disabled via configuration.");
			return;
		}

		final boolean isInitial = initialSyncDone.compareAndSet(false, true);
		if (isInitial) {
			log.info("Starting initial startup threat intelligence sync from live external feeds...");
			threatSyncService.triggerSync(true);
		} else {
			log.info("Starting scheduled 15-minute incremental threat intelligence sync from live feeds...");
			threatSyncService.triggerSync(false);
		}
	}
}
