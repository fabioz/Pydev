package org.python.pydev.plugin;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.eclipse.ui.ide.IDE;

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
	public PydevPlugin() {
		super();
		plugin = this;
	}
	
	public void start(BundleContext context) throws Exception {
		super.start(context);
		try {
			resourceBundle= ResourceBundle.getBundle("org.python.pydev.PyDevPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		Preferences preferences = plugin.getPluginPreferences();
		preferences.addPropertyChangeListener(this);		
	}
	
	public void stop(BundleContext context) throws Exception {
		Preferences preferences = plugin.getPluginPreferences();
		preferences.removePropertyChangeListener(this);
		super.stop(context);
	}

	public static PydevPlugin getDefault() {
		return plugin;
	}
	
	public static String getPluginID() {
		return getDefault().getBundle().getSymbolicName();
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
//		System.out.println( event.getProperty()
//		 + "\n\told setting: "
//		 + event.getOldValue()
//		 + "\n\tnew setting: "
//		 + event.getNewValue());
	}
	/**
	 * @param errorLevel IStatus.[OK|INFO|WARNING|ERROR]
	 */
	public static void log(int errorLevel, String message, Throwable e) {
		Status s = new Status(errorLevel, getPluginID(), errorLevel, message, e);
		getDefault().getLog().log(s);
	}
	
	/**
	 * Utility function that opens an editor on a given path.
	 * 
	 * @return part that is the editor
	 */
	public static IEditorPart doOpenEditor(IPath path, boolean activate) {
		if (path == null)
			return null;
		IWorkspace w = ResourcesPlugin.getWorkspace();
		IFile file = w.getRoot().getFileForLocation(path);
		IWorkbenchPage wp = plugin.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			if (file != null && file.exists()) {
				// File is inside the workspace
				return IDE.openEditor(wp, file, activate);
			} else {
				IStorage storage = new FileStorage(path);
				IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
				IEditorDescriptor desc = registry.getDefaultEditor(path.lastSegment());
				if (desc == null)
					desc = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
				IEditorInput input = new ExternalEditorInput(storage);
				return wp.openEditor(input, desc.getId());
			}
		} catch (PartInitException e) {
			log(IStatus.ERROR, "Unexpected error opening path " + path.toString(),e);
			return null;
		}
	}
}
