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

		// Seed the CBD tenant's social link with the previously-hardcoded footer
		// value so the public footer keeps rendering after the FE switches to the
		// dynamic value. Only fills when unset, so superadmin edits are preserved.
		int socialLink = entityManager.createNativeQuery(
				"UPDATE tenant SET social_link_type = 'INSTAGRAM', " +
						"social_link_url = 'https://www.instagram.com/cbd_sportswear', " +
						"social_link_display_text = 'cbd_sportswear' " +
						"WHERE slug = 'cbd' AND social_link_type IS NULL")
				.executeUpdate();
		if (socialLink > 0) {
			log.info("Seeded social link for CBD tenant");
		}

		// Rename admin role to company_admin if it still exists
		int roles = entityManager.createNativeQuery(
				"UPDATE role SET name = 'company_admin' WHERE name = 'admin'")
				.executeUpdate();
		if (roles > 0) {
			log.info("Renamed 'admin' role to 'company_admin'");
		}

		// Backfill feature flags for tenants that predate the per-tenant feature
		// system (features column still NULL). Before feature-gating existed,
		// EVERY module — including attendance and reports — was reachable by any
		// privileged user, so we enable the full set to avoid revoking access on
		// deploy. The platform admin can disable premium modules per tenant
		// afterwards. New tenants are created with an explicit (non-null) default
		// set, so they are never matched here.
		int features = entityManager.createNativeQuery(
				"UPDATE tenant SET features = 'orders,order-extension,banners,attendance,reports' " +
						"WHERE features IS NULL")
				.executeUpdate();
		if (features > 0) {
			log.info("Backfilled full feature set for {} pre-existing tenant(s)", features);
		}
	}
}
