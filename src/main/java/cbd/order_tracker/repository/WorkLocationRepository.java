package cbd.order_tracker.repository;

import cbd.order_tracker.model.CheckInMethod;
import cbd.order_tracker.model.WorkLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkLocationRepository extends JpaRepository<WorkLocation, Long> {

	@Query("SELECT l FROM WorkLocation l WHERE l.tenant.id = :tenantId")
	List<WorkLocation> findAllByTenant(@Param("tenantId") Long tenantId);

	@Query("SELECT l FROM WorkLocation l WHERE l.tenant.id = :tenantId AND l.active = true")
	List<WorkLocation> findActiveByTenant(@Param("tenantId") Long tenantId);

	@Query("SELECT l FROM WorkLocation l WHERE l.tenant.id = :tenantId AND l.active = true AND l.checkInMethod = :method")
	List<WorkLocation> findActiveByTenantAndMethod(@Param("tenantId") Long tenantId, @Param("method") CheckInMethod method);

	@Query("SELECT l FROM WorkLocation l WHERE l.qrToken = :qrToken AND l.tenant.id = :tenantId")
	Optional<WorkLocation> findByQrTokenAndTenant(@Param("qrToken") String qrToken, @Param("tenantId") Long tenantId);

	boolean existsByQrToken(String qrToken);

	@Query("SELECT l FROM WorkLocation l WHERE l.id = :id AND l.tenant.id = :tenantId")
	Optional<WorkLocation> findByIdAndTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);

	@Query("SELECT COUNT(s) FROM AttendanceSession s WHERE s.location.id = :locationId")
	long countSessionsForLocation(@Param("locationId") Long locationId);
}
