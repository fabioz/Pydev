package org.python.pydev.debug.core;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.python.pydev.ui.ImageCache;
/**
 * The main plugin for Python Debugger.
 * 
 * Standard plugin functionality: preferences, logging, some utility functions
 */
public class PydevDebugPlugin extends AbstractUIPlugin {
	//The shared instance.
	private static PydevDebugPlugin plugin;
	
	public ImageCache imageCache;
	
	public PydevDebugPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
		imageCache = new ImageCache(getDescriptor().getInstallURL());
	}

	public static PydevDebugPlugin getDefault() {
		return plugin;
	}
	
	public static String getPluginID() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}
	
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	public static ImageCache getImageCache() {
		return plugin.imageCache;
	}
	
	protected void initializeDefaultPluginPreferences() {
		PydevDebugPrefs.initializeDefaultPreferences(getPluginPreferences());
	}

	/**
	 * Returns the active workbench window or <code>null</code> if none
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}
	
	public static Status makeStatus(int errorLevel, String message, Throwable e) {
		return new Status(errorLevel, getPluginID(), errorLevel, message, e);
	}
	/**
	 * @param errorLevel  IStatus.[OK|INFO|WARNING|ERROR]
	 */
	public static void log(int errorLevel, String message, Throwable e) {
		Status s = makeStatus(errorLevel, message, e);
		getDefault().getLog().log(s);
	}
	
	public static void errorDialog(final String message, final Throwable t) {
		Display disp = Display.getCurrent();
		disp.asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow window = getDefault().getWorkbench().getActiveWorkbenchWindow();
				Shell shell = window == null ? null : window.getShell();
				if (shell != null) {
					IStatus status= makeStatus(IStatus.ERROR, "Error logged from Pydev Debug: ", t);	
					ErrorDialog.openError(shell, "Its an error", message, status);
				}
			}	
		});
		log(IStatus.ERROR, message, t);
	}

}
