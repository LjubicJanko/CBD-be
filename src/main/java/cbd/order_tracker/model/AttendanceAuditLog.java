package cbd.order_tracker.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
		name = "attendance_audit_log",
		indexes = {
				@Index(name = "idx_attendance_audit_session", columnList = "session_id"),
				@Index(name = "idx_attendance_audit_tenant_created", columnList = "tenant_id,created_at")
		}
)
public class AttendanceAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "session_id", nullable = false)
	private Long sessionId;

	@Column(name = "actor_user_id")
	private Integer actorUserId;

	@Column(name = "actor_username", length = 100)
	private String actorUsername;

	@Column(nullable = false, length = 32)
	private String action;

	@Column(name = "field_name", length = 64)
	private String fieldName;

	@Column(name = "old_value", length = 500)
	private String oldValue;

	@Column(name = "new_value", length = 500)
	private String newValue;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)")
	private LocalDateTime createdAt;
}
