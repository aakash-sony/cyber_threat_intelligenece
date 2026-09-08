package com.cyberthreat.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusResponse {
    private String status; // SUCCESS, RUNNING, PARTIAL, FAILED, IDLE
    private LocalDateTime lastSyncTime;
    private LocalDateTime nextSyncTime;
    private int secondsUntilNextSync;
    private int lastRecordsIngested;
    private int lastRecordsUpdated;
    private int lastRecordsSkipped;
    private int lastRecordsFailed;
    private long lastSyncDurationMs;
    private long totalThreatsInDb;
    private String message;
    private List<ProviderStatusItem> providers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderStatusItem {
        private String name;
        private boolean isLive;
        private String status; // ONLINE, OFFLINE, DISABLED, FAILED
        private int lastBatchCount;
        private int lastFetchedCount;
        private int lastInsertedCount;
        private int lastUpdatedCount;
        private int lastSkippedCount;
        private int lastFailedCount;
        private long durationMs;
        private String feedUrl;
    }
}
