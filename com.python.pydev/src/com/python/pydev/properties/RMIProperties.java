package com.python.pydev.properties;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

public class RMIProperties {
	private final String FILE_NAME = "pydev.properties";
	public static final String IP = "ip";
	public static final String PORT = "portToRead";
	
	private Properties properties;
	
	private static RMIProperties rmiProperties;
	
	private RMIProperties(){
		properties = new Properties();
		try {
			Bundle bundle = Platform.getBundle("com.python.pydev");
			Path path = new Path(FILE_NAME);
			URL fileURL = Platform.find(bundle, path);		
			properties.load( fileURL.openStream() );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static RMIProperties getInstance(){
		if( rmiProperties==null ) {
			return rmiProperties = new RMIProperties();
		}
		return rmiProperties;
	}
	
	public String get( String property ){
		return properties.getProperty( property ); 	
	}
}
