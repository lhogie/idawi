package idawi.security;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSA {
	private Cipher encryptCipher;
	public KeyPair keyPair;

	static Map<PublicKey, Cipher> m = new HashMap<>();

	public RSA(PublicKey k) {
		this.keyPair = new KeyPair(k, null);
		this.encryptCipher = null;
	}

	public RSA() {
	}

	public void random(boolean enableEncryption) {
		try {
			var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			this.keyPair = keyPairGenerator.generateKeyPair();
			
			if (enableEncryption) {
				this.encryptCipher = Cipher.getInstance("RSA");
				encryptCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
			}
			else {
				this.keyPair = new KeyPair(keyPair.getPublic(), null); // remove to enable encryption
			}
		} catch (Throwable err) {
			throw new IllegalStateException(err);
		}
	}

	public byte[] encode(byte[] plainData) {
		try {
			return encryptCipher.doFinal(plainData);
		} catch (Throwable err) {
			throw new IllegalStateException(err);
		}
	}

	public byte[] decode(PublicKey publicKey, byte[] encodedDataBytes) {
		var decryptCipher = m.get(publicKey);

		try {
			if (decryptCipher == null) {
				m.put(publicKey, decryptCipher = Cipher.getInstance("RSA"));
				decryptCipher.init(Cipher.DECRYPT_MODE, publicKey);
			}
			return decryptCipher.doFinal(encodedDataBytes);
		} catch (Throwable err) {
			throw new IllegalStateException(err);
		}
	}

	public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		var g = new RSA();
//		System.out.println(g.keyPair.getPublic().get);
		var encoded = g.encode("salut".getBytes());
		System.out.println(encoded);
		var decoded = g.decode(g.keyPair.getPublic(), encoded);
		System.out.println(new String(decoded));
	}

}
