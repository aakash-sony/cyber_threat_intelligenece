package com.cyberthreat.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates detailed batch ingestion metrics for authentic synchronization logging.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchIngestResult {
	private int fetched;
	private int inserted;
	private int updated;
	private int skipped;
	private int failed;
	private long durationMs;

	public static BatchIngestResult empty() {
		return BatchIngestResult.builder()
				.fetched(0)
				.inserted(0)
				.updated(0)
				.skipped(0)
				.failed(0)
				.durationMs(0)
				.build();
	}
}
