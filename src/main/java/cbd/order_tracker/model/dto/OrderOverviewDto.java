package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

		this.status = orderRecord.getStatus();
		this.priority = orderRecord.getPriority();
		this.executionStatus = orderRecord.getExecutionStatus();

		this.dateWhenMovedToDone = String.valueOf(orderRecord.getDateWhenMovedToDone());
		this.postalCode = orderRecord.getPostalCode();
		this.postalService = orderRecord.getPostalService();

		BigDecimal priceForCalculation = orderRecord.isLegalEntity() ? orderRecord.getSalePriceWithTax() : orderRecord.getSalePrice();
		BigDecimal amountLefToPay = priceForCalculation.subtract(orderRecord.getAmountPaid());
		this.setAmountLeftToPay(amountLefToPay);
	}

	public OrderOverviewDto(
			Long id,
			String name,
			String description,
			LocalDate plannedEndingDate,
			OrderStatus status,
			OrderPriority priority,
			OrderExecutionStatus executionStatus,
			LocalDateTime dateWhenMovedToDone,
			String postalCode,
			String postalService,
			BigDecimal salePrice,
			BigDecimal salePriceWithTax,
			boolean legalEntity,
			BigDecimal amountPaid
	) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.plannedEndingDate = plannedEndingDate;
		this.status = status;
		this.priority = priority;
		this.executionStatus = executionStatus;
		this.dateWhenMovedToDone = dateWhenMovedToDone != null ? String.valueOf(dateWhenMovedToDone) : null;
		this.postalCode = postalCode;
		this.postalService = postalService;

		BigDecimal priceForCalculation = legalEntity ? salePriceWithTax : salePrice;
		BigDecimal amountLeftToPay = priceForCalculation.subtract(amountPaid);
		this.setAmountLeftToPay(amountLeftToPay);
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
