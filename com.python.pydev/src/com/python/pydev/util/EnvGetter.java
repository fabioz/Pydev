package com.python.pydev.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.Location;

import com.python.pydev.PydevPlugin;

public class EnvGetter {
	
	public static Properties getEnvVariables() {
        Properties properties = new Properties();
        properties.put("user.name"     ,getValue("user.name"     ));        
        properties.put("os.name"       ,getValue("os.name"       ));
        properties.put("os.version"    ,getValue("os.version"    ));       
        properties.put("java.io.tmpdir",getValue("java.io.tmpdir"));
        properties.put("user.home"     ,getValue("user.home"     ));
        
        String ext = "null";
        try {
            Location installLocation = Platform.getInstallLocation();
            URL url = installLocation.getURL();
            ext = url.toExternalForm();
        } catch (Exception e) {
        }
        properties.put("InstallLoc",ext);
        
        String location = "null";
        try {
            location = PydevPlugin.getDefault().getBundle().getLocation();
        } catch (Exception e) {
        }
        properties.put("BundleLoc",location);
        
        return properties;
	}

    /**
     * @param properties
     * @return
     */
    public static String getPropertiesStr(Properties properties) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            properties.store(out, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
		return out.toString();
    }
	
	private static String getValue( String property ) {		
		try {
            String data = System.getProperty(property);
            if (data != null) {
                return property+"="+data;
            } else {
                return property+"=null";
            } 
        } catch (Exception e) {
            return property+"=null";
        }		
	}
    
}
