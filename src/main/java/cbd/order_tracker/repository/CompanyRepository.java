package cbd.order_tracker.repository;

import cbd.order_tracker.model.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.employees WHERE c.id = :id")
    Optional<Company> findByIdWithEmployees(@Param("id") Long id);

    Optional<Company> findByName(String name);

}
