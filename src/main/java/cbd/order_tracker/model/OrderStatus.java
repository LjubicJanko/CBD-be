package cbd.order_tracker.model;

public enum OrderStatus {
    DESIGN,
    PRINT_READY,
    PRINTING,
    PRINTED,
    SHIPPED,
    DONE;

    public OrderStatus next() {
        switch (this) {
            case DESIGN: return PRINT_READY;
            case PRINT_READY: return PRINTING;
            case PRINTING: return PRINTED;
            case PRINTED: return SHIPPED;
            case SHIPPED: return DONE;
            default: throw new IllegalStateException("No further transitions allowed from " + this);
        }
    }
}
