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
		name = "attendance_sessions",
		indexes = {
				@Index(name = "idx_attendance_tenant_checkin", columnList = "tenant_id,check_in_at DESC"),
				@Index(name = "idx_attendance_tenant_user_checkin", columnList = "tenant_id,user_id,check_in_at DESC"),
				@Index(name = "idx_attendance_location", columnList = "location_id")
		}
)
public class AttendanceSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tenant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_tenant"))
	@JsonIgnore
	private Tenant tenant;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_user"))
	@JsonIgnore
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "location_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_location"))
	@JsonIgnore
	private WorkLocation location;

	@Column(name = "check_in_at", nullable = false, columnDefinition = "DATETIME(3)")
	private LocalDateTime checkInAt;

	@Column(name = "check_in_lat", nullable = false, precision = 9, scale = 6)
	private BigDecimal checkInLat;

	@Column(name = "check_in_lng", nullable = false, precision = 9, scale = 6)
	private BigDecimal checkInLng;

	@Column(name = "check_in_accuracy_m", nullable = false)
	private Integer checkInAccuracyM;

	@Column(name = "check_in_ip", length = 45)
	private String checkInIp;

	@Column(name = "check_in_user_agent", length = 500)
	private String checkInUserAgent;

	@Column(name = "check_out_at", columnDefinition = "DATETIME(3)")
	private LocalDateTime checkOutAt;

	@Column(name = "check_out_lat", precision = 9, scale = 6)
	private BigDecimal checkOutLat;

	@Column(name = "check_out_lng", precision = 9, scale = 6)
	private BigDecimal checkOutLng;

	@Column(name = "check_out_accuracy_m")
	private Integer checkOutAccuracyM;

	@Column(name = "check_out_ip", length = 45)
	private String checkOutIp;

	@Column(name = "check_out_user_agent", length = 500)
	private String checkOutUserAgent;

	@Column(name = "auto_closed", nullable = false)
	private boolean autoClosed = false;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)")
	private LocalDateTime updatedAt;
}
