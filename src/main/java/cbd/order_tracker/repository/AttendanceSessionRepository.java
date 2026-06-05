package cbd.order_tracker.repository;

import cbd.order_tracker.model.AttendanceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

	@Query("SELECT s FROM AttendanceSession s " +
			"WHERE s.tenant.id = :tenantId AND s.user.id = :userId AND s.checkOutAt IS NULL")
	Optional<AttendanceSession> findOpenForUser(@Param("tenantId") Long tenantId, @Param("userId") Integer userId);

	@Query("SELECT s FROM AttendanceSession s WHERE s.tenant.id = :tenantId AND s.checkOutAt IS NULL AND s.checkInAt < :threshold")
	List<AttendanceSession> findStaleOpenSessions(@Param("tenantId") Long tenantId, @Param("threshold") LocalDateTime threshold);

	@Query("SELECT s FROM AttendanceSession s WHERE s.id = :id AND s.tenant.id = :tenantId")
	Optional<AttendanceSession> findByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

	@Query(
		value =
			"SELECT s FROM AttendanceSession s " +
			"WHERE s.tenant.id = :tenantId " +
			"AND (:fromTs IS NULL OR s.checkInAt >= :fromTs) " +
			"AND (:toTs IS NULL OR s.checkInAt < :toTs) " +
			"AND (:userId IS NULL OR s.user.id = :userId) " +
			"AND (:locationId IS NULL OR s.location.id = :locationId) " +
			"AND (:openOnly = false OR s.checkOutAt IS NULL)",
		countQuery =
			"SELECT COUNT(s) FROM AttendanceSession s " +
			"WHERE s.tenant.id = :tenantId " +
			"AND (:fromTs IS NULL OR s.checkInAt >= :fromTs) " +
			"AND (:toTs IS NULL OR s.checkInAt < :toTs) " +
			"AND (:userId IS NULL OR s.user.id = :userId) " +
			"AND (:locationId IS NULL OR s.location.id = :locationId) " +
			"AND (:openOnly = false OR s.checkOutAt IS NULL)"
	)
	Page<AttendanceSession> search(
			@Param("tenantId") Long tenantId,
			@Param("fromTs") LocalDateTime fromTs,
			@Param("toTs") LocalDateTime toTs,
			@Param("userId") Integer userId,
			@Param("locationId") Long locationId,
			@Param("openOnly") boolean openOnly,
			Pageable pageable
	);
}
