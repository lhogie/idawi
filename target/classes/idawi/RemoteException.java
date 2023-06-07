package idawi;

public class RemoteException extends Exception {

	public RemoteException(String msg) {
		super(msg);
	}

	public RemoteException(Throwable exception) {
		super(exception);
	}

}