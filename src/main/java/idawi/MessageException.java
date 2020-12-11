package idawi;

public class MessageException extends Exception {

	public MessageException(String msg) {
		super(msg);
	}

	public MessageException(Throwable exception) {
		super(exception);
	}

}