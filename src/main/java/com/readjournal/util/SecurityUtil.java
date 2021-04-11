package com.readjournal.util;

import java.io.UnsupportedEncodingException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

public class SecurityUtil {
	private static final Cipher encrypter;
	private static final Cipher decrypter;
	private static final SecretKey key;
	//private static String DES_ENCRYPTION_SCHEME = "DES";
	private static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
	private static final String ENCODING = "UTF-8";
	private static final byte secretBytes[] =
		{ 	71, 	-61, 	-68, 	118, 	101, 	110, 	32, 	89, 	97, 	122, 	-60,
			-79, 	108, 	-60, 	-79, 	109, 	32, 	84, 	101, 	107, 	110, 	111,
			108, 	111, 	106, 	105, 	108, 	101, 	114, 	105,	32, 	76, 	116,
			100, 	46, 	32, 	-59, 	-98, 	116, 	105, 	46 };

	static {
		try {
			//KeySpec keySpec = new DESKeySpec( "Güven Yazılım Teknolojileri Ltd. Şti.".getBytes() );
			KeySpec keySpec = new DESedeKeySpec( secretBytes );
			SecretKeyFactory keyFactory = SecretKeyFactory.getInstance( DESEDE_ENCRYPTION_SCHEME );
			//KeyFactory keyFactory = KeyFactory.getInstance( DESEDE_ENCRYPTION_SCHEME );
			key = keyFactory.generateSecret( keySpec );
			encrypter = Cipher.getInstance( DESEDE_ENCRYPTION_SCHEME );
			encrypter.init(Cipher.ENCRYPT_MODE, key );
			decrypter = Cipher.getInstance( DESEDE_ENCRYPTION_SCHEME );
			decrypter.init(Cipher.DECRYPT_MODE, key );
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	private static String bytesToString(final byte[] buff) {
		try {
			return new String(buff, ENCODING);
		} catch (UnsupportedEncodingException e) {
			return new String(buff);
		}
	}

	private static byte[] stringToBytes(final String str) {
		try {
			return str.getBytes(ENCODING);
		} catch (UnsupportedEncodingException e) {
			return str.getBytes();
		}
	}

	private synchronized static String encrypt(final byte[] buff) {
		try {
			byte[] bytes = encrypter.doFinal( buff );
			return Base64.getEncoder().encodeToString(bytes);
		}
		catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private synchronized static byte[] decrypt(final String str) {
		try {
			byte[] bytes = Base64.getDecoder().decode(str);
			return decrypter.doFinal(bytes);
		}
		catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static String encryptString(final String str ) {
		if( str==null )
			return null;
		byte[] bytes = stringToBytes(str);
		return encrypt( bytes );
	}

	public static String decryptString(final String str) {
		if( str==null )
			return null;
		byte[] bytes = decrypt( str );
		return bytesToString(bytes );
	}

	public static String encryptLong(final long num ) {
		String str = String.valueOf(num);
		return encryptString(str);
	}

	public static long decryptLong(String str) {
		String decStr = decryptString(str);
		return Long.parseLong(decStr);
	}

	public static String encryptInt(final int num ) {
		String str = String.valueOf(num);
		return encryptString(str);
	}

	public static int decryptInt(String str) {
		String decStr = decryptString(str);
		return Integer.parseInt( decStr );
	}

	public static void main(final String[] args) throws Exception {
		String text = "reuters";
		String encrypted = encryptString(text);
		String decrypted = decryptString(encrypted);
		System.out.println("text : " + text);
		System.out.println("encrypted : "+encrypted);
		System.out.println("decrypted : "+decrypted);
	}
}
