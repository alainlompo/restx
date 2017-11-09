package restx.common.exceptions;

public class CryptoException extends RuntimeException {

	private static final long serialVersionUID = 2283006487169921435L;
	
	public CryptoException(Exception e) {
		super(e);
	}
	
}
