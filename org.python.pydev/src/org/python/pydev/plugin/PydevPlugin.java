package org.python.pydev.plugin;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The main plugin class
 * - initialized on startup
 * - has resource bundle for internationalization
 * - has preferences
 */
public class PydevPlugin extends AbstractUIPlugin
						implements Preferences.IPropertyChangeListener {
	
	private static PydevPlugin plugin;	//The shared instance.
	private ResourceBundle resourceBundle;  //Resource bundle.

	/**
	 * The constructor.
	 */
	public PydevPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
	}
	
	public void startup() throws CoreException {
		super.startup();
		try {
			resourceBundle= ResourceBundle.getBundle("org.totic.pydev.PydevPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		Preferences preferences = plugin.getPluginPreferences();
		preferences.addPropertyChangeListener(this);		
	}
	
	public void shutdown() throws CoreException {
		Preferences preferences = plugin.getPluginPreferences();
		preferences.removePropertyChangeListener(this);
		super.shutdown();
	}

	public static PydevPlugin getDefault() {
		return plugin;
	}


	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle= plugin.getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	protected void initializeDefaultPluginPreferences() {
		PydevPrefs.initializeDefaultPreferences(getPluginPreferences());
	}

	public void propertyChange(Preferences.PropertyChangeEvent event) {
		// TODO handle preference changes here
		System.out.println( event.getProperty()
		 + "\n\told setting: "
		 + event.getOldValue()
		 + "\n\tnew setting: "
		 + event.getNewValue());
	}
	
}
