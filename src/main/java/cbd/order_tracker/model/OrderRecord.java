package cbd.order_tracker.model;

import cbd.order_tracker.model.dto.PaymentRequestDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@SQLRestriction("deleted = false")
public class OrderRecord {


	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String description;
	private String note;
	private String plannedEndingDate;
	private String trackingId;
	private String pausingComment;
	private LocalDateTime creationTime;
	private boolean deleted = false;

	@Column(name = "legal_entity", nullable = false)
	private boolean legalEntity;

	@Column(precision = 19, scale = 4)
	private BigDecimal acquisitionCost;

	@Column(precision = 19, scale = 4)
	private BigDecimal salePrice;

	@Column(precision = 19, scale = 4)
	private BigDecimal salePriceWithTax;

	@Column(precision = 19, scale = 4)
	private BigDecimal priceDifference;

	@Column(precision = 19, scale = 4)
	private BigDecimal amountPaid;

	@Column(precision = 19, scale = 4)
	private BigDecimal amountLeftToPay;

	@Column(precision = 19, scale = 4)
	private BigDecimal amountLeftToPayWithTax;

	@Enumerated(EnumType.STRING)
	private OrderStatus status;

	@Enumerated(EnumType.STRING)
	private OrderExecutionStatus executionStatus;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<OrderStatusHistory> statusHistory;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<Payment> payments;

	public OrderRecord() {
	}

	public OrderRecord(OrderRecord order) {
		this.name = order.getName();
		this.description = order.getDescription();
		this.note = order.getNote();
		this.plannedEndingDate = order.getPlannedEndingDate();
		this.trackingId = UUID.randomUUID().toString();
		this.legalEntity = order.isLegalEntity();
		this.acquisitionCost = order.getAcquisitionCost();
		this.salePrice = order.getSalePrice();
		this.salePriceWithTax = order.getSalePrice().multiply(BigDecimal.valueOf(1.2));
		this.priceDifference = order.getSalePrice().subtract(acquisitionCost);
		this.amountPaid = BigDecimal.ZERO;
		this.amountLeftToPay = order.getSalePrice();
		this.amountLeftToPayWithTax = salePriceWithTax;
		this.creationTime = LocalDateTime.now();
		this.status = OrderStatus.DESIGN;
		this.executionStatus = OrderExecutionStatus.ACTIVE;
		this.statusHistory = new ArrayList<>();
		this.payments = new ArrayList<>();
		addStatusHistory(status, null, null);
	}


	public void nextStatus(String postalCode, String postalService) {
		this.status = this.status.next();
		addStatusHistory(status, postalCode, postalService);
	}

	private void addStatusHistory(OrderStatus newStatus, String postalCode, String postalService) {
		statusHistory.add(new OrderStatusHistory(this, newStatus, postalCode, postalService));
	}

	public void addPayment(PaymentRequestDto payment) {
		Payment newPayment = new Payment(this, payment);
		this.payments.add(newPayment);
		this.amountPaid = this.amountPaid.add(newPayment.getAmount());
		this.amountLeftToPay = this.salePrice.subtract(this.amountPaid);
		this.amountLeftToPayWithTax = this.salePriceWithTax.subtract(this.amountPaid);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getPlannedEndingDate() {
		return plannedEndingDate;
	}

	public void setPlannedEndingDate(String plannedEndingDate) {
		this.plannedEndingDate = plannedEndingDate;
	}

	public String getTrackingId() {
		return trackingId;
	}

	public String getPausingComment() {
		return pausingComment;
	}

	public void setPausingComment(String pausingComment) {
		this.pausingComment = pausingComment;
	}

	public void setTrackingId(String trackingId) {
		this.trackingId = trackingId;
	}

	public boolean isLegalEntity() {
		return legalEntity;
	}

	public void setLegalEntity(boolean legalEntity) {
		this.legalEntity = legalEntity;
	}

	public BigDecimal getAcquisitionCost() {
		return acquisitionCost;
	}

	public void setAcquisitionCost(BigDecimal acquisitionCost) {
		this.acquisitionCost = acquisitionCost;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public void setSalePrice(BigDecimal salePrice) {
		this.salePrice = salePrice;
	}

	public BigDecimal getSalePriceWithTax() {
		return salePriceWithTax;
	}

	public void setSalePriceWithTax(BigDecimal salePriceWithTax) {
		this.salePriceWithTax = salePriceWithTax;
	}

	public BigDecimal getPriceDifference() {
		return priceDifference;
	}

	public void setPriceDifference(BigDecimal priceDifference) {
		this.priceDifference = priceDifference;
	}

	public BigDecimal getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(BigDecimal amountPaid) {
		this.amountPaid = amountPaid;
	}

	public BigDecimal getAmountLeftToPay() {
		return amountLeftToPay;
	}

	public void setAmountLeftToPay(BigDecimal amountLeftToPay) {
		this.amountLeftToPay = amountLeftToPay;
	}

	public BigDecimal getAmountLeftToPayWithTax() {
		return amountLeftToPayWithTax;
	}

	public void setAmountLeftToPayWithTax(BigDecimal amountLeftToPayWithTax) {
		this.amountLeftToPayWithTax = amountLeftToPayWithTax;
	}

	public LocalDateTime getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(LocalDateTime creationTime) {
		this.creationTime = creationTime;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public OrderExecutionStatus getExecutionStatus() {
		return executionStatus;
	}

	public void setExecutionStatus(OrderExecutionStatus executionStatus) {
		this.executionStatus = executionStatus;
	}

	public List<OrderStatusHistory> getStatusHistory() {
		return statusHistory;
	}

	public void setStatusHistory(List<OrderStatusHistory> statusHistory) {
		this.statusHistory = statusHistory;
	}

	public List<Payment> getPayments() {
		return payments;
	}

	public void setPayments(List<Payment> payments) {
		this.payments = payments;
	}
}
