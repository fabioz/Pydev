package com.python.pydev.debug;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class DebugPlugin extends AbstractUIPlugin {

    public static final String DEFAULT_PYDEV_DEBUG_SCOPE = "org.python.pydev.debug";
    
    //The shared instance.
    private static DebugPlugin plugin;    
    
    /**
     * The constructor.
     */
    public DebugPlugin() {
        plugin = this;    
    }

    /**
     * This method is called upon plug-in activation
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        new DebugPluginPrefsInitializer().initializeDefaultPreferences();
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
    public static DebugPlugin getDefault() {
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
        return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev.debug", path);
    }
}
