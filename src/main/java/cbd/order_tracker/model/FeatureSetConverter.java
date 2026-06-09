package cbd.order_tracker.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores a tenant's feature keys as a single comma-separated column instead of
 * a join table. The set is tiny (<=5 short keys) and is read on every request
 * by the JWT filter, so co-locating it on the tenant row avoids an extra SELECT
 * (no N+1 on tenant listings) and loads with the row even outside a transaction.
 *
 * <p>Empty set -> empty string (distinct from a legacy NULL column, which the
 * data migration uses to detect tenants that predate feature flags).
 */
@Converter
public class FeatureSetConverter implements AttributeConverter<Set<String>, String> {

	@Override
	public String convertToDatabaseColumn(Set<String> attribute) {
		if (attribute == null || attribute.isEmpty()) {
			return "";
		}
		return String.join(",", attribute);
	}

	@Override
	public Set<String> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return new LinkedHashSet<>();
		}
		return Arrays.stream(dbData.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
