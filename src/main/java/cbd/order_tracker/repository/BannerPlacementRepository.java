package cbd.order_tracker.repository;

import cbd.order_tracker.model.BannerPlacement;
import cbd.order_tracker.model.dto.response.BannerResDto;
import cbd.order_tracker.model.enums.PageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerPlacementRepository extends JpaRepository<BannerPlacement, Long> {
    Optional<BannerPlacement> findByActiveTrueAndPage(PageType page);

    @Query("SELECT bp FROM BannerPlacement bp JOIN FETCH bp.banner WHERE bp.active = true")
    List<BannerPlacement> findAllByActiveTrueWithBanner();

    @Query("""
        SELECT bp
        FROM BannerPlacement bp
        JOIN FETCH bp.banner b
        WHERE bp.active = true
    """)
    List<BannerPlacement> findActivePlacements();

    void deleteByBanner_Id(Long bannerId);

    List<BannerPlacement> findByBanner_Id(Long bannerId);

    List<BannerPlacement> findAllByActiveTrue();
}
