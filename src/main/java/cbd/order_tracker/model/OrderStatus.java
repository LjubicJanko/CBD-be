package cbd.order_tracker.model;

public enum OrderStatus {
    DESIGN,
    PRINT_READY,
    PRINTING,
    SEWING,
    SHIP_READY,
    SHIPPED,
    DONE;

    public OrderStatus next() {
        switch (this) {
            case DESIGN: return PRINT_READY;
            case PRINT_READY: return PRINTING;
            case PRINTING: return SEWING;
            case SEWING: return SHIP_READY;
            case SHIP_READY: return SHIPPED;
            case SHIPPED: return DONE;
            default: throw new IllegalStateException("No further transitions allowed from " + this);
        }
    }
}
