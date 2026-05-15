package cbd.order_tracker.exceptions;

public class TenantNotFoundException extends RuntimeException {
	public TenantNotFoundException(String message) {
		super(message);
	}
}
