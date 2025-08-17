package cbd.order_tracker.repository;

import cbd.order_tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        LEFT JOIN FETCH u.roles r
        LEFT JOIN FETCH r.privileges
        LEFT JOIN FETCH u.companies c
        WHERE u.username = :username
    """)
    Optional<User> findByUsernameWithRolesAndPrivileges(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.privileges")
    List<User> findAllWithRolesAndPrivileges();

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.privileges WHERE u.id IN :ids")
    List<User> findAllByIdWithRolesAndPrivileges(@Param("ids") List<Integer> ids);

    @Query("select c.id from User u join u.companies c where u.username = :username")
    List<Long> findCompanyIdsByUsername(@Param("username") String username);

}