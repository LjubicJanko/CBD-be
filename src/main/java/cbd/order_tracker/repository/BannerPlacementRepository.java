package cbd.order_tracker.repository;

import cbd.order_tracker.model.BannerPlacement;
import cbd.order_tracker.model.dto.response.BannerResDto;
import cbd.order_tracker.model.enums.PageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerPlacementRepository extends JpaRepository<BannerPlacement, Long> {
    @Query("SELECT bp FROM BannerPlacement bp JOIN FETCH bp.banner b WHERE bp.active = true AND bp.page = :page AND b.tenant.id = :tenantId")
    Optional<BannerPlacement> findByActiveTrueAndPage(@Param("page") PageType page, @Param("tenantId") Long tenantId);

    @Query("SELECT bp FROM BannerPlacement bp JOIN FETCH bp.banner b WHERE bp.active = true AND b.tenant.id = :tenantId")
    List<BannerPlacement> findAllByActiveTrueWithBanner(@Param("tenantId") Long tenantId);

    @Query("SELECT bp FROM BannerPlacement bp JOIN FETCH bp.banner b WHERE bp.active = true AND b.tenant.id = :tenantId")
    List<BannerPlacement> findActivePlacements(@Param("tenantId") Long tenantId);

    void deleteByBanner_Id(Long bannerId);

    List<BannerPlacement> findByBanner_Id(Long bannerId);

    @Query("SELECT bp FROM BannerPlacement bp JOIN FETCH bp.banner b WHERE bp.active = true AND b.tenant.id = :tenantId")
    List<BannerPlacement> findAllByActiveTrue(@Param("tenantId") Long tenantId);
}
