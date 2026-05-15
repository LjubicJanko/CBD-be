package cbd.order_tracker.repository;

import cbd.order_tracker.model.Banner;
import cbd.order_tracker.model.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("SELECT DISTINCT b FROM Banner b LEFT JOIN FETCH b.placements WHERE b.tenant.id = :tenantId")
    List<Banner> findAllWithPlacements(@Param("tenantId") Long tenantId);

    @Query("SELECT DISTINCT b FROM Banner b JOIN b.placements p WHERE p.active = true AND b.tenant.id = :tenantId")
    List<Banner> findByPlacements_ActiveTrue(@Param("tenantId") Long tenantId);
}
