package cbd.order_tracker.model;

import cbd.order_tracker.util.UserUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class OrderStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "order_id", nullable = false)
	@JsonIgnore
	private OrderRecord order;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	private LocalDateTime creationTime;
	private String user;

	private String closingComment;
	private String postalCode;
	private String postalService;

	public OrderStatusHistory() {
	}

	public OrderStatusHistory(OrderRecord order, OrderStatus status, String postalCode, String postalService) {
		String currentUser = UserUtil.getCurrentUser();
		this.user = currentUser;
		this.order = order;
		this.status = status;
		this.creationTime = LocalDateTime.now();
		this.postalCode = postalCode;  // Optional
		this.postalService = postalService;  // Optional
	}

	// Getters and setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public OrderRecord getOrder() {
		return order;
	}

	public void setOrder(OrderRecord order) {
		this.order = order;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public LocalDateTime getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(LocalDateTime creationTime) {
		this.creationTime = creationTime;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getClosingComment() {
		return closingComment;
	}

	public void setClosingComment(String closingComment) {
		this.closingComment = closingComment;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getPostalService() {
		return postalService;
	}

	public void setPostalService(String postalService) {
		this.postalService = postalService;
	}
}
