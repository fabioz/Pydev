package com.python.pydev.license;

import java.math.BigInteger;
import java.util.ArrayList;

//encrypt ==> c = m^e( mod N )
// m = data in a number
// e = modulus of public key( in this case of client side )

//decrypt ==> c^e( mod N )
// N = the expoent
// e = modulus of the public key( in the client side )

public class ClientEncryption {
	private static ClientEncryption encryption;
	private BigInteger e;
	private BigInteger N;
	
	public ClientEncryption() {
		e = new BigInteger("65537",10);
		N = new BigInteger("115177032176946546558269068827440200244040503869596632334637862913980482577252368423165152466486515398576152630074226512838661350005676884681271881673730676993314466894521803768688453811901029052598776873607299993786360160003193977375556220882426365859708520873206921482917525578030271496655309864011180862013",10);
	}
	
	public static ClientEncryption getInstance(){
		if( encryption==null ) {
			return encryption = new ClientEncryption();
		} else {
			return encryption;
		}
	}

    protected String[] getChunks(String data) {
        ArrayList<String> strs = new ArrayList<String>();
        while(data.length() > 128){
            strs.add(data.substring(0,128));
            data = data.substring(128);
        }
        if(data.length() > 0){
            strs.add(data);
        }
        return strs.toArray(new String[0]);
    }
    
	public String encrypt(String data) {
        String[] chunks = getChunks(data);
        StringBuffer buf = new StringBuffer();
        for (String string : chunks) {
            BigInteger m = new BigInteger( string.getBytes() );
            BigInteger encrypted = m.modPow( e, N ); 
            buf.append(encrypted.toString());
            buf.append("@");
        }
        return buf.toString();

	}

	public String decrypt(String data) {
        String[] strings = data.split("@");
        StringBuffer buf = new StringBuffer();
        for (String string : strings) {
            BigInteger c = new BigInteger(string);
            BigInteger decrypted = c.modPow(e, N);
            buf.append(new String(decrypted.toByteArray()));
        }
        return buf.toString();
	}	
}
