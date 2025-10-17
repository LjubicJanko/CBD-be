package cbd.order_tracker.model;

public enum OrderStatus {
    PENDING,
    DESIGN,
    PRINT_READY,
    PRINTING,
    SEWING,
    SHIP_READY,
    SHIPPED,
    DONE;

    public OrderStatus next() {
        return switch (this) {
            case PENDING, DESIGN -> PRINT_READY;
            case PRINT_READY -> PRINTING;
            case PRINTING -> SEWING;
            case SEWING -> SHIP_READY;
            case SHIP_READY -> SHIPPED;
            case SHIPPED -> DONE;
            default -> throw new IllegalStateException("No further transitions allowed from " + this);
        };
    }
}
