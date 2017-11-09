package restx.common.exceptions;

public class TemplateRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -8603799174862673078L;
	
	public TemplateRuntimeException(Exception e) {
		super(e);
	}

}
