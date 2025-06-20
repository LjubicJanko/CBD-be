package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.OrderStatus;

import java.time.LocalDateTime;

public class OrderStatusHistoryDTO {

    private Long id;
    private OrderStatus status;
    private String closingComment;
    private LocalDateTime creationTime;
    private String user; // can be full name, or username
    private String postalCode;
    private String postalService;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getClosingComment() {
        return closingComment;
    }

    public void setClosingComment(String closingComment) {
        this.closingComment = closingComment;
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
