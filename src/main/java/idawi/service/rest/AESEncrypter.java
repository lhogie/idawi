package idawi.service.rest;

import java.security.Key;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESEncrypter {

	private static final String ALGO = "AES";
	private static String alphabet = "azertyuiopqsdfghjklmwxcvbn@&é'(§è!çà,;:=?./+$*#1234567890";

	/**
	 * Encrypt byte array using AES algorithm
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public static byte[] encrypt(byte[] data, Key key) throws Exception {
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.ENCRYPT_MODE, key);
		return c.doFinal(data);
	}

	public static byte[] decrypt(byte[] data, Key key) throws Exception {
		Cipher c = Cipher.getInstance(ALGO);
		c.init(Cipher.DECRYPT_MODE, key);
		return c.doFinal(data);
	}

	public static Key generateKey(int nbBits) throws Exception {
		StringBuilder b = new StringBuilder();
		Random r = new Random();

		for (int i = 0; i < nbBits / 8; ++i) {
			b.append(alphabet.charAt(r.nextInt(alphabet.length())));
		}

		return new SecretKeySpec(b.toString().getBytes(), ALGO);
	}

}
