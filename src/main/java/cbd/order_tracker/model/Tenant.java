package cbd.order_tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Entity
public class Tenant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(unique = true, nullable = false)
	private String slug;

	@Column(nullable = false)
	private boolean active = true;

	@CreationTimestamp
	@Column(updatable = false, name = "created_at")
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Lob
	@Column(name = "logo_bytes", columnDefinition = "LONGBLOB")
	@JsonIgnore
	private byte[] logoBytes;

	@Column(name = "logo_content_type", length = 64)
	private String logoContentType;

	// Null when unset. Stored as flat social_link_* columns; Hibernate maps an
	// all-null component back to null.
	@Embedded
	private SocialLink socialLink;

	// Enabled premium feature keys (see Feature enum), stored as a single
	// comma-separated column via FeatureSetConverter. Co-located on the tenant
	// row so it loads with the entity (read by the JWT filter outside a
	// transaction) and avoids an extra SELECT per tenant on listings.
	@Convert(converter = FeatureSetConverter.class)
	@Column(name = "features", length = 255)
	private Set<String> features = new LinkedHashSet<>();

	public Tenant() {}

	public Tenant(String name, String slug) {
		this.name = name;
		this.slug = slug;
	}
}
