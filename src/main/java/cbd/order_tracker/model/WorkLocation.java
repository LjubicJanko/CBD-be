package cbd.order_tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(
		name = "work_locations",
		uniqueConstraints = {
				@UniqueConstraint(name = "uq_work_locations_tenant_name", columnNames = {"tenant_id", "name"})
		},
		indexes = {
				@Index(name = "idx_work_locations_tenant_active", columnList = "tenant_id,active")
		}
)
public class WorkLocation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_work_locations_tenant"))
	@JsonIgnore
	private Tenant tenant;

	@Column(nullable = false, length = 120)
	private String name;

	// Required for GEOFENCE locations; unused (nullable) for QR locations.
	@Column(precision = 9, scale = 6)
	private BigDecimal lat;

	@Column(precision = 9, scale = 6)
	private BigDecimal lng;

	@Column(name = "radius_m")
	private Integer radiusM;

	@Column(nullable = false)
	private boolean active = true;

	// Nullable at the JPA/DDL level so Hibernate's ddl-auto=update never attempts a NOT NULL
	// column add on a populated table (unsafe under MySQL strict mode without a DEFAULT).
	// WorkLocationSchemaInitializer backfills and tightens this to NOT NULL explicitly.
	// New in-memory instances still always get GEOFENCE via the Java default below.
	@Column(name = "check_in_method", length = 20)
	@Enumerated(EnumType.STRING)
	private CheckInMethod checkInMethod = CheckInMethod.GEOFENCE;

	// Required for QR locations (server-generated); unused for GEOFENCE locations.
	@Column(name = "qr_token", unique = true, length = 32)
	private String qrToken;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)")
	private LocalDateTime updatedAt;
}
