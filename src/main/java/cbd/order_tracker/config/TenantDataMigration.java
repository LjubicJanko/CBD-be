package cbd.order_tracker.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class TenantDataMigration implements ApplicationRunner {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		// Ensure default tenant exists before backfilling
		Long tenantCount = ((Number) entityManager.createNativeQuery(
				"SELECT COUNT(*) FROM tenant WHERE id = 1")
				.getSingleResult()).longValue();
		if (tenantCount == 0) {
			entityManager.createNativeQuery(
					"INSERT INTO tenant (id, name, slug, active, created_at, updated_at) VALUES (1, 'CBD', 'cbd', true, NOW(), NOW())")
					.executeUpdate();
			log.info("Created default tenant (id=1)");
		}

		// Backfill tenant_id=1 for existing orders that have no tenant
		int orders = entityManager.createNativeQuery(
				"UPDATE order_record SET tenant_id = 1 WHERE tenant_id IS NULL")
				.executeUpdate();
		if (orders > 0) {
			log.info("Backfilled tenant_id=1 for {} existing orders", orders);
		}

		// Backfill tenant_id=1 for existing banners that have no tenant
		int banners = entityManager.createNativeQuery(
				"UPDATE banner SET tenant_id = 1 WHERE tenant_id IS NULL")
				.executeUpdate();
		if (banners > 0) {
			log.info("Backfilled tenant_id=1 for {} existing banners", banners);
		}

		// Backfill tenant_id=1 for existing users that have no tenant and are not superadmin
		int users = entityManager.createNativeQuery(
				"UPDATE users SET tenant_id = 1 WHERE tenant_id IS NULL AND superadmin = false")
				.executeUpdate();
		if (users > 0) {
			log.info("Backfilled tenant_id=1 for {} existing users", users);
		}

		// Rename admin role to company_admin if it still exists
		int roles = entityManager.createNativeQuery(
				"UPDATE role SET name = 'company_admin' WHERE name = 'admin'")
				.executeUpdate();
		if (roles > 0) {
			log.info("Renamed 'admin' role to 'company_admin'");
		}
	}
}
