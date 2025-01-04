package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.OrderExecutionStatus;
import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;
import cbd.order_tracker.model.OrderStatusHistory;

public class OrderOverviewDto {

	private Long id;
	private String name;
	private String description;
	private String plannedEndingDate;
	private String dateWhenMovedToDone;
	private OrderStatus status;
	private OrderExecutionStatus executionStatus;

	public OrderOverviewDto() {
	}

	public OrderOverviewDto(OrderRecord orderRecord) {
		this.id = orderRecord.getId();
		this.name = orderRecord.getName();
		this.description = orderRecord.getDescription();
		this.plannedEndingDate = orderRecord.getPlannedEndingDate();
		String dateWhenMovedToDone = "";
		var historyRecords = orderRecord.getStatusHistory();

		for (OrderStatusHistory historyRecord : historyRecords) {
			if (historyRecord.getStatus().equals(OrderStatus.DONE)) {
				dateWhenMovedToDone = historyRecord.getCreationTime().toString();
			}
		}

		this.dateWhenMovedToDone = dateWhenMovedToDone;
		this.status = orderRecord.getStatus();
		this.executionStatus = orderRecord.getExecutionStatus();
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

	public String getPlannedEndingDate() {
		return plannedEndingDate;
	}

	public void setPlannedEndingDate(String plannedEndingDate) {
		this.plannedEndingDate = plannedEndingDate;
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

	public String getDateWhenMovedToDone() {
		return dateWhenMovedToDone;
	}

	public void setDateWhenMovedToDone(String dateWhenMovedToDone) {
		this.dateWhenMovedToDone = dateWhenMovedToDone;
	}
}
