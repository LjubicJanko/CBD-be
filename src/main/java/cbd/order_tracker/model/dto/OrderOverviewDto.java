package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;

public class OrderOverviewDto {

    private Long id;
    private String name;
    private String description;
    private String plannedEndingDate;
    private OrderStatus status;

    public OrderOverviewDto() {
    }

    public OrderOverviewDto(OrderRecord orderRecord) {
        this.id = orderRecord.getId();
        this.name = orderRecord.getName();
        this.description = orderRecord.getDescription();
        this.plannedEndingDate = orderRecord.getPlannedEndingDate();
        this.status = orderRecord.getStatus();
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

}
