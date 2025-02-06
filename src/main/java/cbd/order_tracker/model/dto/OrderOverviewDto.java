package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OrderOverviewDto {

	private Long id;
	private String name;
	private String description;
	private LocalDate plannedEndingDate;
	private String dateWhenMovedToDone;
	private OrderStatus status;
	private OrderPriority priority;
	private OrderExecutionStatus executionStatus;
	private BigDecimal amountLeftToPay;
	private String postalCode;
	private String postalService;

	public OrderOverviewDto() {
	}

	public OrderOverviewDto(OrderRecord orderRecord) {
		this.id = orderRecord.getId();
		this.name = orderRecord.getName();
		this.description = orderRecord.getDescription();
		this.plannedEndingDate = orderRecord.getPlannedEndingDate();
		var historyRecords = orderRecord.getStatusHistory();

		for (OrderStatusHistory historyRecord : historyRecords) {
			if (historyRecord.getStatus().equals(OrderStatus.DONE)) {
				this.dateWhenMovedToDone = historyRecord.getCreationTime().toString();
			} else if (historyRecord.getStatus().equals(OrderStatus.SHIPPED)) {
				this.postalCode = historyRecord.getPostalCode();
				this.postalService = historyRecord.getPostalService();
			}
		}

		this.status = orderRecord.getStatus();
		this.priority = orderRecord.getPriority();
		this.executionStatus = orderRecord.getExecutionStatus();

		BigDecimal priceForCalculation = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
		BigDecimal amountLefToPay = priceForCalculation.subtract(orderRecord.getAmountPaid());
		this.setAmountLeftToPay(amountLefToPay);
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

	public OrderPriority getPriority() {
		return priority;
	}

	public void setPriority(OrderPriority priority) {
		this.priority = priority;
	}

	public OrderExecutionStatus getExecutionStatus() {
		return executionStatus;
	}

	public void setExecutionStatus(OrderExecutionStatus executionStatus) {
		this.executionStatus = executionStatus;
	}

	public String getDateWhenMovedToDone() {
		return dateWhenMovedToDone;
	}

	public void setDateWhenMovedToDone(String dateWhenMovedToDone) {
		this.dateWhenMovedToDone = dateWhenMovedToDone;
	}

	public BigDecimal getAmountLeftToPay() {
		return amountLeftToPay;
	}

	public void setAmountLeftToPay(BigDecimal amountLeftToPay) {
		this.amountLeftToPay = amountLeftToPay;
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
