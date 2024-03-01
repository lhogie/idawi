package idawi.security;

import java.security.Key;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AES {

	private static final String ALGO = "AES";

	public static byte[] encode(byte[] data, Key key) {
		try {
			Cipher c = Cipher.getInstance(ALGO);
			c.init(Cipher.ENCRYPT_MODE, key);
			return c.doFinal(data);
		} catch (Throwable err) {
			throw new IllegalStateException(err);
		}
	}

	public static byte[] decode(byte[] data, Key key) {
		try {
			Cipher c = Cipher.getInstance(ALGO);
			c.init(Cipher.DECRYPT_MODE, key);
			return c.doFinal(data);
		} catch (Throwable err) {
			throw new IllegalStateException(err);
		}
	}

	public static Key getRandomKey(int keySize) {
		byte[] randomKeyBytes = new byte[keySize / 8];
		Random random = new Random();
		random.nextBytes(randomKeyBytes);
		return new SecretKeySpec(randomKeyBytes, ALGO);
	}

}
