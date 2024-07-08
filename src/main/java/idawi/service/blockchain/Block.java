package idawi.service.blockchain;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

import toools.security.AES;

public class Block {
	static final AES aes = new AES();
	static final SecretKeyFactory f;

	static {
		try {
			f = SecretKeyFactory.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	final int hash;
	final double timestamp;
	byte[] data;
	final public Block previous;

	public Block(byte[] data, Block prev) {
		this.timestamp = System.nanoTime();
		this.hash = prev == null ? Arrays.hashCode(data)
				: (Arrays.hashCode(data) + ":" + prev.hash + ":" + timestamp).hashCode();
		this.data = f(data, hash, true);
		System.out.println(data);
		this.previous = prev;
	}

	byte[] get(int hash) {
		return f(this.data, hash, false);
	}

	static byte[] f(byte[] data, int hash, boolean encrypt) {
		var ks = new SecretKeySpec(("" + hash).getBytes(), "AES");

		try {
			SecretKey skey = f.generateSecret(ks);
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, skey);
			return cipher.doFinal(data);
		} catch (InvalidKeySpecException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| IllegalBlockSizeException | BadPaddingException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static void main(String[] args) {
		for (Object obj : java.security.Security.getProviders()) {
		    System.out.println(obj);
		}		var b = new Block("hello".getBytes(), null);
	}
}
