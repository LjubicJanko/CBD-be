package cbd.order_tracker.repository;

import cbd.order_tracker.model.OrderStatusHistory;
import cbd.order_tracker.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolesRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}
