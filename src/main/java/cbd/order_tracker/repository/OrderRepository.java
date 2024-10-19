package cbd.order_tracker.repository;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderRecord, Long> {
    Optional<OrderRecord> findByTrackingId(String trackingId);

    List<OrderRecord> findByStatusIn(List<OrderStatus> statuses);

    Iterable<OrderRecord> findByNameContainingOrDescriptionContaining(String searchTerm, String searchTerm1);

    Page<OrderRecord> findAllByStatusIn(List<OrderStatus> statuses, Pageable pageable);
}
