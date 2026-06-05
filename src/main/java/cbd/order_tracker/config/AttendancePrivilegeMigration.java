package cbd.order_tracker.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(100)
public class AttendancePrivilegeMigration implements ApplicationRunner {

	@PersistenceContext
	private EntityManager em;

	private static final List<String> PRIVILEGES = List.of(
			"attendance-check-in",
			"attendance-view-all",
			"attendance-edit",
			"location-manage"
	);

	private static final Map<String, List<String>> DEFAULT_GRANTS = Map.of(
			"attendance-check-in", List.of("company_admin", "manager", "manufacturer"),
			"attendance-view-all", List.of("company_admin", "manager"),
			"attendance-edit", List.of("company_admin"),
			"location-manage", List.of("company_admin")
	);

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		for (String name : PRIVILEGES) {
			ensurePrivilege(name);
		}
		DEFAULT_GRANTS.forEach((priv, roles) -> roles.forEach(role -> ensureGrant(role, priv)));
	}

	private void ensurePrivilege(String name) {
		Number existing = (Number) em.createNativeQuery(
				"SELECT COUNT(*) FROM privilege WHERE name = ?1")
				.setParameter(1, name)
				.getSingleResult();
		if (existing.intValue() > 0) return;

		// The privilege table was seeded by data.sql with explicit ids 1..N, so Hibernate's
		// privilege_seq sequence table starts at 1 and conflicts with existing rows. We
		// can't persist via EntityManager without first resyncing the sequence. Doing the
		// INSERT with an explicit MAX(id)+1 sidesteps the sequence entirely.
		Long nextId = ((Number) em.createNativeQuery(
				"SELECT COALESCE(MAX(id), 0) + 1 FROM privilege")
				.getSingleResult()).longValue();
		em.createNativeQuery("INSERT INTO privilege (id, name) VALUES (?1, ?2)")
				.setParameter(1, nextId)
				.setParameter(2, name)
				.executeUpdate();
		bumpPrivilegeSeq(nextId + 1);
		log.info("Inserted privilege '{}' (id={})", name, nextId);
	}

	// Keep Hibernate's sequence table ahead of MAX(privilege.id) so any future
	// EntityManager.persist(new Privilege(...)) call doesn't collide with our explicit ids.
	// No-op if the sequence table doesn't exist yet.
	private void bumpPrivilegeSeq(long target) {
		Number tableCount = (Number) em.createNativeQuery(
				"SELECT COUNT(*) FROM information_schema.tables " +
				"WHERE table_schema = DATABASE() AND table_name = 'privilege_seq'")
				.getSingleResult();
		if (tableCount.intValue() == 0) return;
		em.createNativeQuery("UPDATE privilege_seq SET next_val = ?1 WHERE next_val < ?1")
				.setParameter(1, target)
				.executeUpdate();
	}

	private void ensureGrant(String roleName, String privilegeName) {
		Number ok = (Number) em.createNativeQuery(
				"SELECT COUNT(*) FROM role r " +
				"JOIN roles_privileges rp ON rp.role_id = r.id " +
				"JOIN privilege p ON p.id = rp.privilege_id " +
				"WHERE r.name = ?1 AND p.name = ?2")
				.setParameter(1, roleName)
				.setParameter(2, privilegeName)
				.getSingleResult();
		if (ok.intValue() > 0) return;

		Number rolePresent = (Number) em.createNativeQuery(
				"SELECT COUNT(*) FROM role WHERE name = ?1")
				.setParameter(1, roleName)
				.getSingleResult();
		if (rolePresent.intValue() == 0) {
			log.warn("Role '{}' not present; skipping grant of '{}'", roleName, privilegeName);
			return;
		}

		// INSERT IGNORE makes this idempotent on concurrent restarts:
		// the composite PK on roles_privileges silently drops the duplicate.
		em.createNativeQuery(
				"INSERT IGNORE INTO roles_privileges (role_id, privilege_id) " +
				"SELECT r.id, p.id FROM role r, privilege p " +
				"WHERE r.name = ?1 AND p.name = ?2")
				.setParameter(1, roleName)
				.setParameter(2, privilegeName)
				.executeUpdate();
		log.info("Granted '{}' to role '{}'", privilegeName, roleName);
	}
}
