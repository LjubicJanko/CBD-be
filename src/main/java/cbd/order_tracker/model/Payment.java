package cbd.order_tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private OrderRecord order;

    private String payer; // For individual: name and surname, for legal entity: company name

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount; // Amount in RSD

    @Column(nullable = false)
    private LocalDate paymentDate; // Payment date, defaults to today

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod; // Payment method options: Account, Cash, Invoice

    private String note; // Optional field for additional notes

    public Payment() {}

    public Payment(OrderRecord order, String payer, BigDecimal amount, PaymentMethod paymentMethod, String note) {
        this.order = order;
        this.payer = payer;
        this.amount = amount;
        this.paymentDate = LocalDate.now();
        this.paymentMethod = paymentMethod;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public OrderRecord getOrder() {
        return order;
    }

    public void setOrder(OrderRecord order) {
        this.order = order;
    }

    public String getPayer() {
        return payer;
    }

    public void setPayer(String payer) {
        this.payer = payer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
