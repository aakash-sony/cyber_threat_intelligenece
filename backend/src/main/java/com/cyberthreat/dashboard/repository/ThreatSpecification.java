package com.cyberthreat.dashboard.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.cyberthreat.dashboard.dto.request.ThreatFilterRequest;
import com.cyberthreat.dashboard.entity.ThreatEntity;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public class ThreatSpecification {

	public static Specification<ThreatEntity> withFilter(final ThreatFilterRequest filter) {
		return (root, query, criteriaBuilder) -> {
			final List<Predicate> predicates = new ArrayList<>();

			if (filter == null)
				return criteriaBuilder.conjunction();

			// Keyword search across indicator, description, country, source, target, tags with SQL wildcard escaping
			if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
				final var sanitized = filter.getKeyword().trim().toLowerCase()
						.replace("\\", "\\\\")
						.replace("%", "\\%")
						.replace("_", "\\_");
				final var pattern = "%" + sanitized + "%";
				final var indicatorPred 		= criteriaBuilder.like(criteriaBuilder.lower(root.get("indicator")), pattern, '\\');
				final var descPred 				= criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern, '\\');
				final var countryPred 			= criteriaBuilder.like(criteriaBuilder.lower(root.get("country")), pattern, '\\');
				final var sourcePred 			= criteriaBuilder.like(criteriaBuilder.lower(root.get("source")), pattern, '\\');
				final var targetPred 			= criteriaBuilder.like(criteriaBuilder.lower(root.get("target")), pattern, '\\');
				final var tagsPred 				= criteriaBuilder.like(criteriaBuilder.lower(root.get("tags")), pattern, '\\');

				predicates.add(criteriaBuilder.or(indicatorPred, descPred, countryPred, sourcePred, targetPred, tagsPred));
			}

			if (filter.getSeverity() != null)
				predicates.add(criteriaBuilder.equal(root.get("severity"), filter.getSeverity()));

			if (filter.getThreatType() != null)
				predicates.add(criteriaBuilder.equal(root.get("threatType"), filter.getThreatType()));

			if (filter.getIndicatorType() != null)
				predicates.add(criteriaBuilder.equal(root.get("indicatorType"), filter.getIndicatorType()));

			if (filter.getStatus() != null)
				predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));

			if (filter.getCountry() != null && !filter.getCountry().trim().isEmpty()
					&& !"all".equalsIgnoreCase(filter.getCountry().trim())
					&& !"all countries".equalsIgnoreCase(filter.getCountry().trim())) {
				final var countryVal = filter.getCountry().trim().toLowerCase();
				if ("unknown".equals(countryVal)) {
					predicates.add(criteriaBuilder.or(
							criteriaBuilder.isNull(root.get("country")),
							criteriaBuilder.equal(criteriaBuilder.lower(root.get("country")), "unknown"),
							criteriaBuilder.equal(root.get("country"), "")
					));
				} else {
					predicates.add(criteriaBuilder.equal(
							criteriaBuilder.lower(root.get("country")),
							countryVal
					));
				}
			}

			if (filter.getSource() != null && !filter.getSource().trim().isEmpty()
					&& !"all".equalsIgnoreCase(filter.getSource().trim()))
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("source")),
						filter.getSource().trim().toLowerCase()
						));

			if (filter.getTarget() != null && !filter.getTarget().trim().isEmpty()
					&& !"all".equalsIgnoreCase(filter.getTarget().trim()))
				predicates.add(criteriaBuilder.equal(
						criteriaBuilder.lower(root.get("target")),
						filter.getTarget().trim().toLowerCase()
						));

			// Time range filtering
			var since = filter.getSince();
			if (since == null && filter.getTimeRange() != null && !filter.getTimeRange().isBlank()) {
				final var now = java.time.LocalDateTime.now();
				since = switch (filter.getTimeRange().toLowerCase().trim()) {
					case "24h" -> now.minusHours(24);
					case "7d", "week" -> now.minusDays(7);
					case "30d", "1m", "month" -> now.minusDays(30);
					case "90d", "3m", "3 months" -> now.minusDays(90);
					case "1y", "year" -> now.minusYears(1);
					case "all", "all time" -> null;
					default -> null;
				};
			}

			if (since != null) {
				final Expression<LocalDateTime> timestampExpr = criteriaBuilder.<LocalDateTime>coalesce(root.get("lastSeen"), root.get("createdAt"));
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(timestampExpr, since));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}
