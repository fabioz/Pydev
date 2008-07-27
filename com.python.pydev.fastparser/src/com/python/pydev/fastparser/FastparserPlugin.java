package com.python.pydev.fastparser;

import org.eclipse.ui.plugin.*;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.BundleContext;

import com.python.pydev.PydevPlugin;

/**
 * The main plugin class to be used in the desktop.
 */
public class FastparserPlugin extends AbstractUIPlugin {

	//The shared instance.
	private static FastparserPlugin plugin;
	
	/**
	 * The constructor.
	 */
	public FastparserPlugin() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		PydevPlugin.getDefault().checkValid();
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
	public static FastparserPlugin getDefault() {
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
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev.fastparser", path);
	}
}
