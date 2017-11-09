package restx.common.processor.exceptions;

public class AbstractProcessorException extends RuntimeException {

	private static final long serialVersionUID = 3092971255704914455L;
	
	public AbstractProcessorException(Exception e) {
		super(e);
	}

}
