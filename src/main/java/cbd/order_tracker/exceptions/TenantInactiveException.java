package cbd.order_tracker.exceptions;

public class TenantInactiveException extends RuntimeException {
	public TenantInactiveException(String message) {
		super(message);
	}
}
