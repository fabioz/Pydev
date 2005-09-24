package com.python.pydev.analysis;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.ImageCache;

/**
 * The main plugin class to be used in the desktop.
 */
public class AnalysisPlugin extends AbstractUIPlugin {

	//The shared instance.
	private static AnalysisPlugin plugin;
    private ImageCache imageCache;
	
	/**
	 * The constructor.
	 */
	public AnalysisPlugin() {
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
	public static AnalysisPlugin getDefault() {
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
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev.analysis", path);
	}


    public ImageCache getImageCache() {
        if(imageCache == null){
            imageCache = new ImageCache(AnalysisPlugin.getDefault().getBundle().getEntry("/"));
        }
        return imageCache;
    }
    
    
}
