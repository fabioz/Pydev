package com.python.pydev.encryption;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import org.python.pydev.core.REF;
import org.python.pydev.licensemanager.encryption.AbstractEncryption;

public class PydevExtensionEncryption extends AbstractEncryption {
	private static final String	UNICODE_FORMAT = "UTF8";
	private static PydevExtensionEncryption encryption;
	private String m = "8690429699402548205970876624079338052037199844476879315450388947370323179551492802278114552518560302867055695788220513351875983157936595211376880014910012";
	private KeySpec keySpec;
	private SecretKeyFactory keyFactory;
	private Cipher cipher;
	
	public PydevExtensionEncryption() {
		try {
			keySpec = new DESedeKeySpec( m.getBytes() );
			keyFactory = SecretKeyFactory.getInstance( "DESede" );
			cipher = Cipher.getInstance( "DESede" );
		} catch (InvalidKeyException e) {		
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}		
	}
	
	public static PydevExtensionEncryption getInstance() {
		if( encryption==null ) {
			return encryption = new PydevExtensionEncryption();			
		}
		return encryption;
	}
	
	@Override
	public String encrypt(String unencryptedString) {	
		try {		
			SecretKey key = keyFactory.generateSecret( keySpec );
			cipher.init( Cipher.ENCRYPT_MODE, key );
			byte[] cleartext = unencryptedString.getBytes( UNICODE_FORMAT );
			byte[] ciphertext = cipher.doFinal( cleartext );

			System.out.println("encrypt:" + new String(REF.encodeBase64( ciphertext )));
			return new String(REF.encodeBase64( ciphertext ));
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}		
		return null;
	}

	@Override
	public String decrypt(String encryptedString) {
		SecretKey key;
		try {
			key = keyFactory.generateSecret( keySpec );
			cipher.init( Cipher.DECRYPT_MODE, key );
			byte[] cleartext = REF.decodeBase64( encryptedString );
			byte[] ciphertext = cipher.doFinal( cleartext );
			System.out.println("decrypt:" + new String(ciphertext));
			return new String( ciphertext );
		} catch (InvalidKeySpecException e) {		
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}		
		return null;
	}
}
