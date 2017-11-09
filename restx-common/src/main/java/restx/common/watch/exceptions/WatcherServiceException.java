package restx.common.watch.exceptions;

public class WatcherServiceException extends RuntimeException {

	private static final long serialVersionUID = -8543056763639999361L;
	
	public WatcherServiceException(Exception e) {
		super(e);
	}

}
