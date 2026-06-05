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

	@Column(nullable = false, precision = 9, scale = 6)
	private BigDecimal lat;

	@Column(nullable = false, precision = 9, scale = 6)
	private BigDecimal lng;

	@Column(name = "radius_m", nullable = false)
	private Integer radiusM;

	@Column(nullable = false)
	private boolean active = true;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)")
	private LocalDateTime updatedAt;
}
