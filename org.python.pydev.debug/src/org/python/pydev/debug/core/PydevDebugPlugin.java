package org.python.pydev.debug.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.python.pydev.debug.unittest.ITestRunListener;
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

	/** Listener list **/
    private List listeners = new ArrayList();

	public PydevDebugPlugin() {
		plugin = this;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		imageCache = new ImageCache(PydevDebugPlugin.getDefault().getBundle().getEntry("/"));		
	}

	public static PydevDebugPlugin getDefault() {
		return plugin;
	}
	
	public static String getPluginID() {
		return getDefault().getBundle().getSymbolicName();
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
		Display disp = Display.getDefault();
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

    /**
     * 
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static File getScriptWithinPySrc(String targetExec)
            throws CoreException {
    
        IPath relative = new Path("pysrc").addTrailingSeparator().append(
                targetExec);
    
        Bundle bundle = getDefault().getBundle();
    
        URL bundleURL = Platform.find(bundle, relative);
        URL fileURL;
        try {
            fileURL = Platform.asLocalURL(bundleURL);
            File f = new File(fileURL.getPath());
    
            return f;
        } catch (IOException e) {
            throw new CoreException(makeStatus(IStatus.ERROR,
                    "Can't find python debug script", null));
        }
    }

    /**
     * 
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static File getPySrcPath()
            throws CoreException {
    
        IPath relative = new Path("pysrc");
    
        Bundle bundle = getDefault().getBundle();
    
        URL bundleURL = Platform.find(bundle, relative);
        URL fileURL;
        try {
            fileURL = Platform.asLocalURL(bundleURL);
            File f = new File(fileURL.getPath());
    
            return f;
        } catch (IOException e) {
            throw new CoreException(makeStatus(IStatus.ERROR,
                    "Can't find python debug script", null));
        }
    }
	public void addTestListener(ITestRunListener listener) {
		listeners.add(listener);
	}
	
	public void removeTestListener(ITestRunListener listener) {
		listeners.remove(listener);
	}

	public List getListeners() {
		return listeners;
	}
	
	public void fireTestsStarted(int count, String path_to_file) {
		for (Iterator all=getListeners().iterator(); all.hasNext();) {
			ITestRunListener each = (ITestRunListener) all.next();
			each.testsStarted(count, path_to_file);
		}
	}

	public void fireTestsFinished(String summary) {
		for (Iterator all=getListeners().iterator(); all.hasNext();) {
			ITestRunListener each = (ITestRunListener) all.next();
			each.testsFinished(summary);
		}
	}

	public void fireTestStarted(String klass, String methodName) {
		for (Iterator all=getListeners().iterator(); all.hasNext();) {
			ITestRunListener each = (ITestRunListener) all.next();
			each.testStarted(klass, methodName);
		}
	}

	public void fireTestOK(String klass, String methodName) {
		for (Iterator all=getListeners().iterator(); all.hasNext();) {
			ITestRunListener each = (ITestRunListener) all.next();
			each.testOK(klass, methodName);
		}
	}

	public void fireTestFailed(String klass, String methodName, String failureType, String trace) {
		for (Iterator all=getListeners().iterator(); all.hasNext();) {
			ITestRunListener each = (ITestRunListener) all.next();
			each.testFailed(klass, methodName, failureType, trace);
		}
	}

}
