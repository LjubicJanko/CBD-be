package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class OrderDTO {

	private Long id;
	private String name;
	private String description;
	private String note;
	private LocalDate plannedEndingDate;
	private String pausingComment;
	private OrderStatus status;
	private OrderPriority priority;
	private OrderExecutionStatus executionStatus;
	private String trackingId;
	private List<OrderStatusHistoryDTO> statusHistory;

	private List<Payment> payments;

	private boolean isLegalEntity;

	// Use BigDecimal for monetary values
	private BigDecimal acquisitionCost;
	private BigDecimal salePrice;
	private BigDecimal salePriceWithTax;
	private BigDecimal priceDifference;
	private BigDecimal amountPaid;
	private BigDecimal amountLeftToPay; // this should be calculated based on info if it is legal entity or not

	public OrderDTO() {
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public LocalDate getPlannedEndingDate() {
		return plannedEndingDate;
	}

	public void setPlannedEndingDate(LocalDate plannedEndingDate) {
		this.plannedEndingDate = plannedEndingDate;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public String getTrackingId() {
		return trackingId;
	}

	public void setTrackingId(String trackingId) {
		this.trackingId = trackingId;
	}

	public List<OrderStatusHistoryDTO> getStatusHistory() {
		return statusHistory;
	}

	public void setStatusHistory(List<OrderStatusHistoryDTO> statusHistory) {
		this.statusHistory = statusHistory;
	}

	public boolean isLegalEntity() {
		return isLegalEntity;
	}

	public void setLegalEntity(boolean legalEntity) {
		isLegalEntity = legalEntity;
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

	public List<Payment> getPayments() {
		return payments;
	}

	public void setPayments(List<Payment> payments) {
		this.payments = payments;
	}

	public String getPausingComment() {
		return pausingComment;
	}

	public void setPausingComment(String pausingComment) {
		this.pausingComment = pausingComment;
	}

	public OrderExecutionStatus getExecutionStatus() {
		return executionStatus;
	}

	public void setExecutionStatus(OrderExecutionStatus executionStatus) {
		this.executionStatus = executionStatus;
	}

	public OrderPriority getPriority() {
		return priority;
	}

	public void setPriority(OrderPriority priority) {
		this.priority = priority;
	}
}
