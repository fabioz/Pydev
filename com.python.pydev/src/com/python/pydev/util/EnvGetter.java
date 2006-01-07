package com.python.pydev.util;

import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;

import com.python.pydev.PydevPlugin;

public class EnvGetter {
	
	public static String getEnvVariables() {
        String data = "";
        data += getValue("user.name");        
        data += getValue("os.name");
        data += getValue("os.version");       
        data += getValue("java.io.tmpdir");
        data += getValue("user.home");
        
        String ext = "null";
        try {
            Location installLocation = Platform.getInstallLocation();
            URL url = installLocation.getURL();
            ext = url.toExternalForm();
        } catch (Exception e) {
        }
        data += "InstallLoc="+ext + "\n";
        
        String location = "null";
        try {
            location = PydevPlugin.getDefault().getBundle().getLocation();
        } catch (Exception e) {
        }
        data += "BundleLoc="+location+"\n";
        
		return data;
	}
	
	private static String getValue( String property ) {		
		try {
            String data = System.getProperty(property);
            if (data != null) {
                return property+"="+data+"\n";
            } else {
                return property+"=null\n";
            } 
        } catch (Exception e) {
            return property+"=null\n";
        }		
	}
    
}
