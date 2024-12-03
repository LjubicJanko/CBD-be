package cbd.order_tracker.repository;

import cbd.order_tracker.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	void delete(Payment paymentToDelete);
}
