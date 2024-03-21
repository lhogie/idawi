package idawi.service;

import idawi.Component;
import idawi.Service;
import toools.security.RSAEncoder;

public class EncryptionService extends Service {
	public RSAEncoder rsa;

	public EncryptionService(Component component) {
		super(component);
		this.rsa = new RSAEncoder(component.keyPair);
	}
}
