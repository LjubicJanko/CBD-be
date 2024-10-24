package cbd.order_tracker.model.dto;

import cbd.order_tracker.model.OrderRecord;
import cbd.order_tracker.model.OrderStatus;

import java.math.BigDecimal;

public class OrderTrackingDTO {

    private String name;
    private String description;
    private String plannedEndingDate;
    private OrderStatus status;
    private boolean isLegalEntity;
    private BigDecimal amountLeftToPay;
    private BigDecimal amountLeftToPayWithTax;
    private String postalCode;
    private String postalService;

    public OrderTrackingDTO() {
    }

    public OrderTrackingDTO(OrderRecord orderRecord) {
        this.name = orderRecord.getName();
        this.description = orderRecord.getDescription();
        this.plannedEndingDate = orderRecord.getPlannedEndingDate();
        this.status = orderRecord.getStatus();
        this.isLegalEntity = orderRecord.isLegalEntity();
        this.amountLeftToPay = orderRecord.getAmountLeftToPay();
        this.amountLeftToPayWithTax = orderRecord.getAmountLeftToPayWithTax();
        if (orderRecord.getStatus() == OrderStatus.SHIPPED) {
            var history = orderRecord.getStatusHistory();
            var lastStatusHistoryChange = history.get(history.size() - 1);
            this.postalCode = lastStatusHistoryChange.getPostalCode();
            this.postalService = lastStatusHistoryChange.getPostalService();
        }
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

    public boolean isLegalEntity() {
        return isLegalEntity;
    }

    public void setLegalEntity(boolean legalEntity) {
        isLegalEntity = legalEntity;
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