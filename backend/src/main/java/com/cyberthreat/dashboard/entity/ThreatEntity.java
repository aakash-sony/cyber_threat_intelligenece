package com.cyberthreat.dashboard.entity;

import java.time.LocalDateTime;

import com.cyberthreat.dashboard.enums.IndicatorType;
import com.cyberthreat.dashboard.enums.Severity;
import com.cyberthreat.dashboard.enums.ThreatStatus;
import com.cyberthreat.dashboard.enums.ThreatType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
		name = "threats",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_threat_indicator_source", columnNames = {"indicator", "indicator_type", "source"})
		},
		indexes = {
				@Index(name = "idx_threat_severity", columnList = "severity"),
				@Index(name = "idx_threat_type", columnList = "threat_type"),
				@Index(name = "idx_threat_status", columnList = "status"),
				@Index(name = "idx_threat_last_seen", columnList = "last_seen"),
				@Index(name = "idx_threat_source", columnList = "source"),
				@Index(name = "idx_threat_created_at", columnList = "created_at"),
				@Index(name = "idx_threat_country", columnList = "country"),
				@Index(name = "idx_threat_indicator", columnList = "indicator"),
				@Index(name = "idx_threat_indicator_type", columnList = "indicator_type")
		}
		)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreatEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 500)
	private String indicator;

	@Enumerated(EnumType.STRING)
	@Column(name = "indicator_type", nullable = false, length = 30)
	private IndicatorType indicatorType;

	@Enumerated(EnumType.STRING)
	@Column(name = "threat_type", nullable = false, length = 50)
	private ThreatType threatType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Severity severity;

	@Column(nullable = false)
	private Integer confidence;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(length = 100)
	private String country;

	@Column(name = "country_code", length = 10)
	private String countryCode;

	@Column(name = "geo_source", length = 100)
	private String geoSource;

	@Column(nullable = false, length = 100)
	private String source;

	@Column(name = "source_url", length = 1000)
	private String sourceUrl;

	@Column(length = 100)
	private String target;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ThreatStatus status;

	@Column(length = 500)
	private String tags;

	@Column(name = "first_seen")
	private LocalDateTime firstSeen;

	@Column(name = "last_seen")
	private LocalDateTime lastSeen;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
		if (status == null)
			status = ThreatStatus.ACTIVE;
		if (confidence == null)
			confidence = 80;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}
}
