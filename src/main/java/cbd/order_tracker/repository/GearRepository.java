package cbd.order_tracker.repository;

import cbd.order_tracker.model.config.Gear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GearRepository extends JpaRepository<Gear, Long> {
}
