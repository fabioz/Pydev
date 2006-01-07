package com.python.pydev.util;

public class EnvGetter {
	
	public static String getEnvVariables() {
        String data = "";
        data += getValue("user.name");        
        data += getValue("os.name");
        data += getValue("os.version");       
        data += getValue("java.io.tmpdir");
        data += getValue("user.home");
		return data;
	}
	
	private static String getValue( String property ) {		
		try {
            String data = System.getProperty(property);
            if (data != null) {
                return data+"\n";
            } else {
                return "null"+"\n";
            } 
        } catch (Exception e) {
            return "null"+"\n";
        }		
	}
    
    public static void main(String[] args) {
        System.out.println(getEnvVariables());
    }
}
