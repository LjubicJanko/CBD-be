package cbd.order_tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

	public Tenant() {}

	public Tenant(String name, String slug) {
		this.name = name;
		this.slug = slug;
	}
}
