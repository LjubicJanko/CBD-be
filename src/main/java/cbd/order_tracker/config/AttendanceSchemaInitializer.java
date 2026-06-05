package cbd.order_tracker.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(50)
public class AttendanceSchemaInitializer implements ApplicationRunner {

	@PersistenceContext
	private EntityManager em;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!tableExists("attendance_sessions")) {
			// Hibernate hasn't created the table yet (e.g., ddl-auto=none in some envs);
			// nothing to do — operator must run the migration SQL.
			log.warn("attendance_sessions table does not exist; skipping generated-column setup");
			return;
		}

		// Add the generated "open_user_id" column that is non-null only while the session is open.
		// A unique index on (tenant_id, open_user_id) then enforces "at most one open session per
		// user per tenant" — MySQL's substitute for Postgres partial unique indexes.
		// Requires MySQL 8.0.13+.
		if (!columnExists("attendance_sessions", "open_user_id")) {
			em.createNativeQuery(
					"ALTER TABLE attendance_sessions " +
					"ADD COLUMN open_user_id INT AS (CASE WHEN check_out_at IS NULL THEN user_id END) STORED"
			).executeUpdate();
			log.info("Added generated column attendance_sessions.open_user_id");
		}

		if (!indexExists("attendance_sessions", "uq_attendance_open_session")) {
			em.createNativeQuery(
					"ALTER TABLE attendance_sessions " +
					"ADD UNIQUE KEY uq_attendance_open_session (tenant_id, open_user_id)"
			).executeUpdate();
			log.info("Added unique index uq_attendance_open_session on attendance_sessions");
		}

		// Safety net: the "at most one open session per user" invariant is enforced ONLY by this
		// index — check-in's findOpenForUser pre-check is not race-safe, so a missing index would
		// silently allow duplicate open sessions. Fail startup loudly rather than run without it.
		if (!indexExists("attendance_sessions", "uq_attendance_open_session")) {
			throw new IllegalStateException(
					"Required unique index uq_attendance_open_session is missing on attendance_sessions; " +
					"the one-open-session-per-user guarantee cannot be enforced. Aborting startup.");
		}
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
