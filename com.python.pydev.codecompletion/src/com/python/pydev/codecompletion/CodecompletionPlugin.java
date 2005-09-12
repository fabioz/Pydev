package com.python.pydev.codecompletion;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.ui.ImageCache;

/**
 * The main plugin class to be used in the desktop.
 */
public class CodecompletionPlugin extends AbstractUIPlugin {

	//The shared instance.
	private static CodecompletionPlugin plugin;

    
    
	/**
	 * The constructor.
	 */
	public CodecompletionPlugin() {
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
	public static CodecompletionPlugin getDefault() {
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
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev.codecompletion", path);
	}

    private static ImageCache imageCache;
    

    public static ImageCache getImageCache() {
        if(imageCache == null){
            imageCache = new ImageCache(CodecompletionPlugin.getDefault().getBundle().getEntry("/"));
        }
        return imageCache;
    }
    
    public static final String CLASS_WITH_IMPORT_ICON = "icons/class_obj_imp.gif";
    public static final String METHOD_WITH_IMPORT_ICON = "icons/method_obj_imp.gif";

}
