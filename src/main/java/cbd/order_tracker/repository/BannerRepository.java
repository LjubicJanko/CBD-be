package cbd.order_tracker.repository;

import cbd.order_tracker.model.Banner;
import cbd.order_tracker.model.OrderRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    @Query("SELECT DISTINCT b FROM Banner b LEFT JOIN FETCH b.placements")
    List<Banner> findAllWithPlacements();


    Optional<Banner> findByPlacements_ActiveTrue();
}
