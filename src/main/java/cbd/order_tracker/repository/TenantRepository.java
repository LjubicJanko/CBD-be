package cbd.order_tracker.repository;

import cbd.order_tracker.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
	Optional<Tenant> findBySlug(String slug);
}
