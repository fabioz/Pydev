package com.python.pydev.browsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.eclipse.ui.internal.util.BundleUtility;
import org.eclipse.ui.plugin.*;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class BrowsingPlugin extends AbstractUIPlugin {

	//The shared instance.
	private static BrowsingPlugin plugin;
	
	/**
     * Storage for dialog and wizard data; <code>null</code> if not yet
     * initialized.
     */
    private DialogSettings dialogSettings = null;
    
    /**
     * The name of the dialog settings file (value 
     * <code>"dialog_settings.xml"</code>).
     */
    private static final String FN_DIALOG_SETTINGS = "dialog_settings.xml"; //$NON-NLS-1$
    
    /**
     * Storage for preferences.
     */
    private ScopedPreferenceStore preferenceStore;
	
	/**
	 * The constructor.
	 */
	public BrowsingPlugin() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
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
	public static BrowsingPlugin getDefault() {
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
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev.browsing", path);
	}
	
	/**
     * Returns the dialog settings for this UI plug-in.
     * The dialog settings is used to hold persistent state data for the various
     * wizards and dialogs of this plug-in in the context of a workbench. 
     * <p>
     * If an error occurs reading the dialog store, an empty one is quietly created
     * and returned.
     * </p>
     * <p>
     * Subclasses may override this method but are not expected to.
     * </p>
     *
     * @return the dialog settings
     */
    public IDialogSettings getDialogSettings() {
        if (dialogSettings == null)
            loadDialogSettings();
        return dialogSettings;
    }
    
    /**
     * Loads the dialog settings for this plug-in.
     * The default implementation first looks for a standard named file in the 
     * plug-in's read/write state area; if no such file exists, the plug-in's
     * install directory is checked to see if one was installed with some default
     * settings; if no file is found in either place, a new empty dialog settings
     * is created. If a problem occurs, an empty settings is silently used.
     * <p>
     * This framework method may be overridden, although this is typically
     * unnecessary.
     * </p>
     */
    protected void loadDialogSettings() {
        dialogSettings = new DialogSettings("Workbench"); //$NON-NLS-1$

        // bug 69387: The instance area should not be created (in the call to
        // #getStateLocation) if -data @none or -data @noDefault was used
        IPath dataLocation = getStateLocationOrNull();
        if (dataLocation != null) {
	        // try r/w state area in the local file system
	        String readWritePath = dataLocation.append(FN_DIALOG_SETTINGS)
	                .toOSString();
	        File settingsFile = new File(readWritePath);
	        if (settingsFile.exists()) {
	            try {
	                dialogSettings.load(readWritePath);
	            } catch (IOException e) {
	                // load failed so ensure we have an empty settings
	                dialogSettings = new DialogSettings("Workbench"); //$NON-NLS-1$
	            }
	            
	            return;
	        }
        }

        // otherwise look for bundle specific dialog settings
        URL dsURL = BundleUtility.find(getBundle(), FN_DIALOG_SETTINGS);
        if (dsURL == null)
            return;

        InputStream is = null;
        try {
            is = dsURL.openStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, "utf-8")); //$NON-NLS-1$
            dialogSettings.load(reader);
        } catch (IOException e) {
            // load failed so ensure we have an empty settings
            dialogSettings = new DialogSettings("Workbench"); //$NON-NLS-1$
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }    
    
    private IPath getStateLocationOrNull() {
        try {
            return getStateLocation();
        } catch (IllegalStateException e) {
            // This occurs if -data=@none is explicitly specified, so ignore this silently.
            // Is this OK? See bug 85071.
            return null;
        }
    }
    
    /**
     * Returns the preference store for this UI plug-in.
     * This preference store is used to hold persistent settings for this plug-in in
     * the context of a workbench. Some of these settings will be user controlled, 
     * whereas others may be internal setting that are never exposed to the user.
     * <p>
     * If an error occurs reading the preference store, an empty preference store is
     * quietly created, initialized with defaults, and returned.
     * </p>
     * <p>
     * <strong>NOTE:</strong> As of Eclipse 3.1 this method is
     * no longer referring to the core runtime compatibility layer and so
     * plug-ins relying on Plugin#initializeDefaultPreferences
     * will have to access the compatibility layer themselves.
     * </p>
     *
     * @return the preference store 
     */
    public IPreferenceStore getPreferenceStore() {
        // Create the preference store lazily.
        if (preferenceStore == null) {
            preferenceStore = new ScopedPreferenceStore(new InstanceScope(),getBundle().getSymbolicName());

        }
        return preferenceStore;
    }
}
