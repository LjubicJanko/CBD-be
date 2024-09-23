package cbd.order_tracker.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
public class OrderRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String trackingId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderStatusHistory> statusHistory;

    public OrderRecord(){};

    public OrderRecord(String name, String description, String user) {
        this.name = name;
        this.description = description;
        this.status = OrderStatus.DESIGN;  // initial state
        this.statusHistory = new ArrayList<>();
        this.trackingId = UUID.randomUUID().toString();
        addStatusHistory(status, user);  // log the initial state
    }

    public void nextStatus(String user) {
        this.status = this.status.next();
        addStatusHistory(status, user);  // log the status change
    }

    private void addStatusHistory(OrderStatus newStatus, String user) {
        statusHistory.add(new OrderStatusHistory(this, newStatus, user));
    }

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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<OrderStatusHistory> getStatusHistory() {
        return statusHistory;
    }

    public void setStatusHistory(List<OrderStatusHistory> statusHistory) {
        this.statusHistory = statusHistory;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    // Getters and Setters
}
