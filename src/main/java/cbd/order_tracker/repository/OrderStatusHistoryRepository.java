package cbd.order_tracker.repository;

import cbd.order_tracker.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderId(Long orderId);

    @Query("SELECT h FROM OrderStatusHistory h WHERE h.order.id IN :orderIds")
    List<OrderStatusHistory> findByOrderIdIn(@Param("orderIds") Collection<Long> orderIds);
}
