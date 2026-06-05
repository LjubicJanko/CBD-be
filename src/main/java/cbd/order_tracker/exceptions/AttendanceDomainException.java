package cbd.order_tracker.exceptions;

public class AttendanceDomainException extends RuntimeException {

	public enum Reason {
		OUT_OF_GEOFENCE("out_of_geofence"),
		ALREADY_CHECKED_IN("already_checked_in"),
		NOT_CHECKED_IN("not_checked_in"),
		NO_ACTIVE_LOCATIONS("no_active_locations");

		private final String code;

		Reason(String code) {
			this.code = code;
		}

		public String code() {
			return code;
		}
	}

	private final Reason reason;

	public AttendanceDomainException(Reason reason, String message) {
		super(message);
		this.reason = reason;
	}

	public Reason getReason() {
		return reason;
	}
}
