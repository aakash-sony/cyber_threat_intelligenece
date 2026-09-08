package com.cyberthreat.dashboard.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cyberthreat.dashboard.dto.response.BatchIngestResult;
import com.cyberthreat.dashboard.dto.response.SyncStatusResponse;
import com.cyberthreat.dashboard.repository.ThreatRepository;
import com.cyberthreat.dashboard.service.ThreatIntelligenceProvider;
import com.cyberthreat.dashboard.service.ThreatService;
import com.cyberthreat.dashboard.service.ThreatSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreatSyncServiceImpl implements ThreatSyncService {

	private final List<ThreatIntelligenceProvider> providers;
	private final ThreatService threatService;
	private final ThreatRepository threatRepository;

	@Value("${threat.scheduler.fixed-rate-ms:900000}")
	private long fixedRateMs;

	private final AtomicBoolean isSyncRunning = new AtomicBoolean(false);
	private volatile LocalDateTime lastSyncTime = null;
	private volatile LocalDateTime nextSyncTime = null;
	private volatile String syncStatus = "IDLE";
	private volatile int lastRecordsIngested = 0;
	private volatile int lastRecordsUpdated = 0;
	private volatile int lastRecordsSkipped = 0;
	private volatile int lastRecordsFailed = 0;
	private volatile long lastSyncDurationMs = 0;
	private volatile String lastSyncMessage = "System initialized. Waiting for scheduled background sync.";

	private final Map<String, BatchIngestResult> providerResults = new ConcurrentHashMap<>();
	private final Map<String, String> providerErrorMessages = new ConcurrentHashMap<>();

	@Override
	public SyncStatusResponse getSyncStatus() {
		final var now = LocalDateTime.now();
		var secondsRemaining = 0;
		if (nextSyncTime != null && nextSyncTime.isAfter(now)) {
			secondsRemaining = (int) Duration.between(now, nextSyncTime).getSeconds();
		}

		final List<SyncStatusResponse.ProviderStatusItem> providerItems = new ArrayList<>();
		for (final ThreatIntelligenceProvider provider : providers) {
			final var isLive = provider.isLiveFeed();
			final var result = providerResults.getOrDefault(provider.getProviderName(), BatchIngestResult.empty());
			final var error = providerErrorMessages.get(provider.getProviderName());
			final String statusStr;
			if (!isLive) {
				statusStr = "DISABLED";
			} else if (error != null) {
				statusStr = "FAILED";
			} else if (lastSyncTime != null) {
				statusStr = "ONLINE";
			} else {
				statusStr = "READY";
			}

			providerItems.add(SyncStatusResponse.ProviderStatusItem.builder()
					.name(provider.getProviderName())
					.isLive(isLive)
					.status(statusStr)
					.lastBatchCount(result.getInserted() + result.getUpdated())
					.lastFetchedCount(result.getFetched())
					.lastInsertedCount(result.getInserted())
					.lastUpdatedCount(result.getUpdated())
					.lastSkippedCount(result.getSkipped())
					.lastFailedCount(result.getFailed())
					.durationMs(result.getDurationMs())
					.feedUrl(provider.getFeedUrl())
					.build());
		}

		return SyncStatusResponse.builder()
				.status(isSyncRunning.get() ? "RUNNING" : syncStatus)
				.lastSyncTime(lastSyncTime)
				.nextSyncTime(nextSyncTime)
				.secondsUntilNextSync(secondsRemaining)
				.lastRecordsIngested(lastRecordsIngested)
				.lastRecordsUpdated(lastRecordsUpdated)
				.lastRecordsSkipped(lastRecordsSkipped)
				.lastRecordsFailed(lastRecordsFailed)
				.lastSyncDurationMs(lastSyncDurationMs)
				.totalThreatsInDb(threatRepository.count())
				.message(lastSyncMessage)
				.providers(providerItems)
				.build();
	}

	@Override
	public SyncStatusResponse triggerSync() {
		return triggerSync(false);
	}

	@Override
	public SyncStatusResponse triggerSync(final boolean isInitialSync) {
		if (!isSyncRunning.compareAndSet(false, true)) {
			log.info("Threat intelligence synchronization is already in progress. Skipping concurrent trigger.");
			return getSyncStatus();
		}

		final var startTime = LocalDateTime.now();
		final long startNs = System.currentTimeMillis();
		log.info("================================================================================");
		log.info(">>> THREAT INTELLIGENCE SYNC CYCLE STARTED at {} [mode: {}] <<<",
				startTime, isInitialSync ? "INITIAL" : "INCREMENTAL_15M");
		log.info("================================================================================");

		var totalInserted = 0;
		var totalUpdated = 0;
		var totalSkipped = 0;
		var totalFailed = 0;
		var successfulProviders = 0;
		var failedProviders = 0;

		try {
			for (final ThreatIntelligenceProvider provider : providers) {
				if (!provider.isLiveFeed()) {
					log.debug("Provider {} is disabled. Skipping.", provider.getProviderName());
					continue;
				}

				final var name = provider.getProviderName();
				final var providerStart = LocalDateTime.now();
				log.info("----------------------------------------------------------------");
				log.info("Provider: {}", name);
				log.info("Sync started: {}", providerStart);

				try {
					final var threats = provider.fetchThreats(isInitialSync);
					final int fetched = (threats != null) ? threats.size() : 0;

					final BatchIngestResult ingestResult;
					if (threats != null && !threats.isEmpty()) {
						ingestResult = threatService.batchIngestDetailed(threats);
					} else {
						ingestResult = BatchIngestResult.empty();
					}

					providerResults.put(name, ingestResult);
					providerErrorMessages.remove(name);

					totalInserted += ingestResult.getInserted();
					totalUpdated += ingestResult.getUpdated();
					totalSkipped += ingestResult.getSkipped();
					totalFailed += ingestResult.getFailed();
					successfulProviders++;

					final var providerEnd = LocalDateTime.now();
					log.info("Sync completed: {}", providerEnd);
					log.info("Records fetched:  {}", fetched);
					log.info("Records inserted: {}", ingestResult.getInserted());
					log.info("Records updated:  {}", ingestResult.getUpdated());
					log.info("Records skipped:  {}", ingestResult.getSkipped());
					log.info("Records failed:   {}", ingestResult.getFailed());
					log.info("Duration:         {} ms", ingestResult.getDurationMs());
					log.info("----------------------------------------------------------------");
				} catch (final Exception e) {
					failedProviders++;
					providerErrorMessages.put(name, e.getMessage());
					providerResults.put(name, BatchIngestResult.empty());
					log.error("Provider {} synchronization failed: {}. Continuing with remaining providers.", name, e.getMessage(), e);
				}
			}

			lastSyncTime = LocalDateTime.now();
			final var rateMinutes = Math.max(1, fixedRateMs / (60 * 1000));
			nextSyncTime = lastSyncTime.plusMinutes(rateMinutes);
			lastRecordsIngested = totalInserted;
			lastRecordsUpdated = totalUpdated;
			lastRecordsSkipped = totalSkipped;
			lastRecordsFailed = totalFailed;
			lastSyncDurationMs = System.currentTimeMillis() - startNs;

			if (failedProviders == 0 && successfulProviders > 0) {
				syncStatus = "SUCCESS";
				lastSyncMessage = String.format("Sync completed successfully in %d ms. Ingested %d new, updated %d existing records across %d providers.",
						lastSyncDurationMs, totalInserted, totalUpdated, successfulProviders);
			} else if (successfulProviders > 0) {
				syncStatus = "PARTIAL";
				lastSyncMessage = String.format("Sync completed with warnings in %d ms. Ingested %d new, updated %d existing records (%d providers succeeded, %d failed).",
						lastSyncDurationMs, totalInserted, totalUpdated, successfulProviders, failedProviders);
			} else {
				syncStatus = "FAILED";
				lastSyncMessage = "All configured external threat APIs failed or were unreachable. Existing database records remain preserved.";
			}

			log.info("================================================================================");
			log.info(">>> THREAT INTELLIGENCE SYNC CYCLE COMPLETED in {} ms <<<", lastSyncDurationMs);
			log.info("Total Inserted: {}, Total Updated: {}, Total Skipped: {}, Total Failed: {}, Status: {}",
					totalInserted, totalUpdated, totalSkipped, totalFailed, syncStatus);
			log.info("================================================================================");
		} catch (final Exception e) {
			log.error("Fatal error during threat intelligence synchronization: {}", e.getMessage(), e);
			syncStatus = "FAILED";
			lastSyncMessage = "Fatal ingestion error: " + e.getMessage();
		} finally {
			isSyncRunning.set(false);
		}

		return getSyncStatus();
	}
}
