package cbd.order_tracker.repository;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends CrudRepository<OrderRecord, Long> {
    Optional<OrderRecord> findByTrackingId(String trackingId);

    List<OrderRecord> findByStatusIn(List<OrderStatus> statuses);

    Iterable<OrderRecord> findByNameContainingOrDescriptionContaining(String searchTerm, String searchTerm1);
}
