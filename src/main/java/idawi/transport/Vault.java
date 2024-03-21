package idawi.transport;

import java.io.Serializable;
import java.security.Key;
import java.security.PublicKey;

import toools.io.ser.JavaSerializer;
import toools.io.ser.Serializer;
import toools.security.AES;
import toools.security.RSAEncoder;

public class Vault implements Serializable {
	public final byte[] encryptedAESKey;
	public final byte[] encryptedContent;

	public Vault(Object content, RSAEncoder rsa, Serializer ser) {
		var aesKey = AES.getRandomKey(128);
		encryptedAESKey = rsa.encode(ser.toBytes(aesKey));
		encryptedContent = AES.encode(ser.toBytes(content), aesKey);
	}

	public byte[] decode(PublicKey pk) {
		var plainAESKey = RSAEncoder.decode(pk, encryptedAESKey);
		Key aesKey = (Key) new JavaSerializer().fromBytes(plainAESKey);
		return AES.decode(encryptedContent, aesKey);
	}
}