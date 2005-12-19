package com.python.pydev.util;

public class EnvGetter {
	private static String USER_NAME = "user.name";
	private static String OS_NAME = "os.name";
	private static String OS_VERSION = "os.version";
	private static String USER_LANGUAGE = "user.language";
	private static String PATCH_LEVEL = "sun.os.patch.level";	
	private static String VM_VERSION = "java.vm.version";
	
	public static String getEnvVariables() {
		String data = "";
		data += getValue( USER_NAME );	
		data += getValue( OS_NAME );
		data += getValue( OS_VERSION );
		data += getValue( USER_LANGUAGE );
		data += getValue( PATCH_LEVEL );		
		data += getValue( VM_VERSION );
		
		return data;
	}
	
	private static String getValue( String property ) {		
		String data = System.getProperty(property);
		if( data!=null ) {
			return  data + "\n";
		} else {
			return "null" + "\n";
		}		
	}
}
