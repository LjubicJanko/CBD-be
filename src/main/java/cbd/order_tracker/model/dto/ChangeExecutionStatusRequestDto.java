package cbd.order_tracker.model.dto;


import cbd.order_tracker.model.OrderExecutionStatus;

public class ChangeExecutionStatusRequestDto {
    private OrderExecutionStatus executionStatus;
    private String note;

    // Getters and Setters
    public OrderExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(OrderExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
