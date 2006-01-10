package com.python.pydev;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.python.pydev.core.REF;

import com.python.pydev.license.ClientEncryption;
import com.python.pydev.util.EnvGetter;
import com.python.pydev.util.PydevExtensionNotifier;

/**
 * The main plugin class to be used in the desktop.
 */
public class PydevPlugin extends AbstractUIPlugin {

	//The shared instance.
	private static PydevPlugin plugin;
	private PydevExtensionNotifier notifier;
	private boolean validated;
	
	/**
	 * The constructor.
	 */
	public PydevPlugin() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		checkValid();
	}

    public boolean isValidated(){
        return validated;
    }
    
	public String checkValidStr() {
	    String result = loadLicense();
        if(notifier == null){
            notifier = new PydevExtensionNotifier();
            notifier.setPriority(Thread.MIN_PRIORITY);
            notifier.start();
        }
        return result;
        
    }
	public boolean checkValid() {
        checkValidStr();
	    return validated;
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static PydevPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev", path);
	}
	
	
	public void saveLicense( String data ) {	
		Bundle bundle = Platform.getBundle("com.python.pydev");
		IPath path = Platform.getStateLocation( bundle );		
    	path = path.addTrailingSeparator();
    	path = path.append("license");
    	try {
			FileOutputStream file = new FileOutputStream(path.toFile());
			file.write( data.getBytes() );
			file.close();
		} catch (FileNotFoundException e) {		
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}   			
    }
	
	private String loadLicense() {
    	Bundle bundle = Platform.getBundle("com.python.pydev");
    	IPath path = Platform.getStateLocation( bundle );		
    	path = path.addTrailingSeparator();
    	path = path.append("license");
    	try {
            File f = path.toFile();
            if(!f.exists()){
                throw new FileNotFoundException("File not found.");
            }
            
            String encLicense = REF.getFileContents(f);
			if( isLicenseValid(encLicense) ) {
				validated = true;
			} else {
				validated = false;
			}
		} catch (FileNotFoundException e) {
			validated = false;
			String ret = "The license file: "+path.toOSString()+" was not found.";
            return ret;
            
		} catch (Exception e) {
		    validated = false;
            return e.getMessage();
		}
        return null;
	}

    private boolean isLicenseValid(String encLicense) {
        //already decrypted
        String license = ClientEncryption.getInstance().decrypt(encLicense);
        try {
            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(license.getBytes()));
            
            String eMail = (String) properties.remove("e-mail");
            String name  = (String) properties.remove("name");
            String time = (String) properties.remove("time");
            
            if(eMail == null && name == null && time == null){
                throw new RuntimeException("The license is not correct, please re-paste it. If this error persists, please request a new license.");
            }
            
            if(!getPreferenceStore().getString(PydevExtensionInitializer.USER_EMAIL).equals(eMail)){
                throw new RuntimeException("The e-mail specified is different from the e-mail in the license.");
            }
            getPreferenceStore().setValue(PydevExtensionInitializer.USER_NAME, name);
            getPreferenceStore().setValue(PydevExtensionInitializer.LIC_TIME, time);
            Properties envVariables = EnvGetter.getEnvVariables();
            
            String bund1 = (String) properties.remove("BundleLoc");
            String bund2 = (String) envVariables.remove("BundleLoc");
            String[] bundVersion = getBundVersion(bund1);
            String[] bundVersion2 = getBundVersion(bund2);
            
            //don't take the version into consideration when checking this...
            if(!bundVersion[0].equals(bundVersion2[0])){
            	throw new RuntimeException("The license was generated for the e-mail provided, but not for this specific installation.\nPlease request a new license for this installation.");
            }
            
            if(!envVariables.equals(properties)){
                throw new RuntimeException("The license was generated for the e-mail provided, but not for this specific installation.\nPlease request a new license for this installation.");
            }
            
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        
        //if it got here, everything is ok...
        return true;
    }

	private String[] getBundVersion(String bund) {
		String pluginName = "com.python.pydev";
		int i = bund.indexOf(pluginName);
		String loc = bund.substring(0, i+pluginName.length());
		String version = bund.substring(i+pluginName.length(), bund.length());
		return new String[]{loc, version};
	}
	
}
