package cbd.order_tracker.repository;

import cbd.order_tracker.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RolesRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.privileges WHERE r.name = :name")
    Optional<Role> findByNameWithPrivileges(@Param("name") String name);
}