package cbd.order_tracker.repository;

import cbd.order_tracker.model.config.GenericConfig;
import cbd.order_tracker.model.enumerations.ConfigType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenericConfigRepository extends JpaRepository<GenericConfig, Long> {
    List<GenericConfig> findByType(ConfigType type);

    void deleteByType(String type);
}
