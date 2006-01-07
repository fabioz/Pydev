package com.python.pydev;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

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

	public boolean checkValid() {
		loadLicense();
		if( !validated ) {
			if(notifier == null){
				notifier = new PydevExtensionNotifier();
				notifier.setValidated( false );
				notifier.start();
			}
		}else{
			notifier.setValidated(true);
		}
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
	
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		store.setDefault( PydevExtensionInitializer.USER_NAME_VALIDATE_EXTENSION, "" );
		store.setDefault( PydevExtensionInitializer.LICENSE_NUMBER_VALIDATE_EXTENSION, "" );
		
		store.setValue( PydevExtensionInitializer.USER_NAME_VALIDATE_EXTENSION, "" );
		store.setValue( PydevExtensionInitializer.LICENSE_NUMBER_VALIDATE_EXTENSION, "" );
	}
	
	public void saveLicense( String data ) {	
		Bundle bundle = Platform.getBundle("com.python.pydev");
		IPath path = Platform.getStateLocation( bundle );		
    	path = path.addTrailingSeparator();
    	path = path.append("license");
    	try {
			FileOutputStream file = new FileOutputStream(path.toString());
			file.write( data.getBytes() );
			file.close();
		} catch (FileNotFoundException e) {		
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}   			
    }
	
	private void loadLicense() {
		if(!validated){
	    	Bundle bundle = Platform.getBundle("com.python.pydev");
	    	IPath path = Platform.getStateLocation( bundle );		
	    	path = path.addTrailingSeparator();
	    	path = path.append("license");
	    	try {
				FileInputStream in = new FileInputStream(path.toString());
				byte code[] = new byte[in.available()];
				in.read(code);
				in.close();
				String license = new String(code);
				license = ClientEncryption.getInstance().decrypt(license);
				System.out.println("loadLicense: " + license);
				
				String name = license.substring( 0, license.indexOf("\n") );
				String temp = license.substring( license.indexOf("\n")+1 );
				String lic = temp.substring( 0, temp.indexOf("\n") );
				temp = temp.substring( temp.indexOf("\n") + 1 );
				
				if( temp.equals(EnvGetter.getEnvVariables()) ) {
					getPreferenceStore().setValue(PydevExtensionInitializer.USER_NAME_VALIDATE_EXTENSION,name);
					getPreferenceStore().setValue(PydevExtensionInitializer.LICENSE_NUMBER_VALIDATE_EXTENSION,lic);
					validated = true;
				} else {
					validated = false;
				}
			} catch (FileNotFoundException e) {
				validated = false;
				System.err.println("The license file: "+path.toOSString()+" was not found.");
			} catch (IOException e) {
				validated = false;
				e.printStackTrace();
			}
		}
	}
	
}
