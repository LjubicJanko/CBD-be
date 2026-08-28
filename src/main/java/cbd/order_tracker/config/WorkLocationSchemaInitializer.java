package cbd.order_tracker.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent structural setup for the QR check-in feature on work_locations.
 * Same rationale as {@link AttendanceSchemaInitializer}: ddl-auto=update adds
 * missing columns but won't reliably relax NOT NULL on a populated table or
 * add a unique index, so those steps are done explicitly here.
 */
@Slf4j
@Component
@Order(60)
public class WorkLocationSchemaInitializer implements ApplicationRunner {

	@PersistenceContext
	private EntityManager em;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!tableExists("work_locations")) {
			log.warn("work_locations table does not exist; skipping QR check-in column setup");
			return;
		}

		// check_in_method: the entity maps this column as nullable (see WorkLocation) so
		// Hibernate only ever adds it as NULL — safe on a populated table. We then backfill
		// and tighten to NOT NULL ourselves, in explicit, dependency-free steps.
		if (!columnExists("work_locations", "check_in_method")) {
			em.createNativeQuery(
					"ALTER TABLE work_locations ADD COLUMN check_in_method VARCHAR(20) NULL"
			).executeUpdate();
			log.info("Added work_locations.check_in_method (nullable)");
		}
		int backfilled = em.createNativeQuery(
				"UPDATE work_locations SET check_in_method = 'GEOFENCE' WHERE check_in_method IS NULL"
		).executeUpdate();
		if (backfilled > 0) {
			log.info("Backfilled check_in_method = GEOFENCE for {} existing work_locations rows", backfilled);
		}
		if (columnNullable("work_locations", "check_in_method")) {
			em.createNativeQuery(
					"ALTER TABLE work_locations MODIFY COLUMN check_in_method VARCHAR(20) NOT NULL"
			).executeUpdate();
			log.info("Tightened work_locations.check_in_method to NOT NULL");
		}

		if (!columnExists("work_locations", "qr_token")) {
			em.createNativeQuery(
					"ALTER TABLE work_locations ADD COLUMN qr_token VARCHAR(32) NULL"
			).executeUpdate();
			log.info("Added work_locations.qr_token");
		}

		if (!indexExists("work_locations", "uq_work_locations_qr_token")) {
			em.createNativeQuery(
					"ALTER TABLE work_locations ADD UNIQUE KEY uq_work_locations_qr_token (qr_token)"
			).executeUpdate();
			log.info("Added unique index uq_work_locations_qr_token on work_locations");
		}

		// lat/lng/radius_m are only required for GEOFENCE locations now; QR locations leave
		// them null. Relax the NOT NULL constraints inherited from the geofence-only schema.
		relaxNotNull("work_locations", "lat", "DECIMAL(9,6)");
		relaxNotNull("work_locations", "lng", "DECIMAL(9,6)");
		relaxNotNull("work_locations", "radius_m", "INT");
	}

	private void relaxNotNull(String table, String column, String columnType) {
		if (columnNullable(table, column)) {
			return;
		}
		em.createNativeQuery(
				"ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + columnType + " NULL"
		).executeUpdate();
		log.info("Relaxed {}.{} to nullable", table, column);
	}

	private boolean columnNullable(String table, String column) {
		String isNullable = (String) em.createNativeQuery(
				"SELECT is_nullable FROM information_schema.columns " +
				"WHERE table_schema = DATABASE() AND table_name = ?1 AND column_name = ?2")
				.setParameter(1, table)
				.setParameter(2, column)
				.getSingleResult();
		return "YES".equalsIgnoreCase(isNullable);
	}

	private boolean tableExists(String table) {
		Number n = (Number) em.createNativeQuery(
				"SELECT COUNT(*) FROM information_schema.tables " +
				"WHERE table_schema = DATABASE() AND table_name = ?1")
				.setParameter(1, table)
				.getSingleResult();
		return n.intValue() > 0;
	}

	private boolean columnExists(String table, String column) {
		Number n = (Number) em.createNativeQuery(
				"SELECT COUNT(*) FROM information_schema.columns " +
				"WHERE table_schema = DATABASE() AND table_name = ?1 AND column_name = ?2")
				.setParameter(1, table)
				.setParameter(2, column)
				.getSingleResult();
		return n.intValue() > 0;
	}

	private boolean indexExists(String table, String indexName) {
		Number n = (Number) em.createNativeQuery(
				"SELECT COUNT(*) FROM information_schema.statistics " +
				"WHERE table_schema = DATABASE() AND table_name = ?1 AND index_name = ?2")
				.setParameter(1, table)
				.setParameter(2, indexName)
				.getSingleResult();
		return n.intValue() > 0;
	}
}
