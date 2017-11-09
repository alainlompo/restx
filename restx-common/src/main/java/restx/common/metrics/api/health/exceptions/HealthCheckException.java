package restx.common.metrics.api.health.exceptions;

public class HealthCheckException extends Exception {
	
	private static final long serialVersionUID = 5820423270117598994L;

	public HealthCheckException(String message) {
		super(message);
	}
}
