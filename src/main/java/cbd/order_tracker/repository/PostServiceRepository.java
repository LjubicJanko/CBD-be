package cbd.order_tracker.repository;

import cbd.order_tracker.model.post_service.PostService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostServiceRepository extends JpaRepository<PostService, Long> {
}
