package com.cyberthreat.dashboard.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Entity representing cached IP geolocation metadata resolved from external public geolocation providers.
 * Note: IP Geolocation represents infrastructure/network location, not physical attacker location.
 */
@Entity
@Table(
		name = "ip_geolocation",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_ip_geolocation_ip", columnNames = {"ip"})
		},
		indexes = {
				@Index(name = "idx_ip_geolocation_ip", columnList = "ip"),
				@Index(name = "idx_ip_geolocation_country", columnList = "country_name")
		}
		)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpGeolocationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 64)
	private String ip;

	@Column(name = "country_code", length = 10)
	private String countryCode;

	@Column(name = "country_name", length = 100)
	private String countryName;

	@Column(length = 100)
	private String region;

	@Column(length = 100)
	private String city;

	private Double latitude;

	private Double longitude;

	@Column(length = 100)
	private String asn;

	@Column(length = 255)
	private String organization;

	@Column(length = 100)
	private String source;

	@Column(name = "last_updated", nullable = false)
	private LocalDateTime lastUpdated;

	@PrePersist
	protected void onCreate() {
		if (lastUpdated == null)
			lastUpdated = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		lastUpdated = LocalDateTime.now();
	}
}
